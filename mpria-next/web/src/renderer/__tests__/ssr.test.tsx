import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';

import { render } from '@/renderer/render';

import { boundTo, field, group, model, region, slot } from './fixtures';

/**
 * The layout must be right in the HTML the server sends, not once JavaScript has run.
 *
 * <p>A renderer that measures the viewport can only guess during server rendering, so the first
 * paint is wrong and then reflows — a visible flash for everyone, and a permanently wrong layout
 * for anything that never runs the script. Resolving the cascade into custom properties makes the
 * server's output complete, and this test is what proves it stays that way.
 */
describe('server-rendered output', () => {
  const pm = model(
    [
      region(
        'region-1',
        [
          group('group-1', [slot('slot-1', boundTo('fieldA'), { base: 1, md: 2 })], {
            span: { base: 1, md: 6, xl: 8 },
            grid: { columns: { base: 1, md: 2 } },
          }),
        ],
        { columns: { base: 1, md: 6, xl: 12 }, gap: 'md' },
      ),
    ],
    { fieldA: field({ required: true }) },
  );

  const html = renderToStaticMarkup(render(pm));

  it('carries the resolved cascade for every breakpoint', () => {
    expect(html).toContain('--mp-cols-md:6');
    expect(html).toContain('--mp-cols-xl:12');
    expect(html).toContain('--mp-span-xl:8');
  });

  it('carries the accessibility wiring', () => {
    expect(html).toContain('aria-required="true"');
    expect(html).toMatch(/<label[^>]+for="mp-slot-1-control"/);
  });

  it('contains no inline script deciding the layout', () => {
    expect(html).not.toContain('<script');
  });
});
