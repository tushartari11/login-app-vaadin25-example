import { render as mount } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { render } from '@/renderer/render';

import { boundTo, cssVariable, field, group, model, region, slot } from './fixtures';

function nested() {
  return model(
    [
      region('region-1', [
        group(
          'group-1',
          [
            slot('slot-1', boundTo('fieldA'), { base: 1 }),
            slot('slot-2', boundTo('fieldB'), { base: 1, md: 2 }),
          ],
          {
            label: 'label.group1',
            span: { base: 1, md: 6, xl: 8 },
            grid: { columns: { base: 1, md: 2 }, gap: 'md' },
          },
        ),
        slot('slot-3', { type: 'MEDIA_GALLERY' }, { base: 1, md: 6, xl: 4 }),
      ], { columns: { base: 1, md: 6, xl: 12 }, gap: 'lg' }),
    ],
    { fieldA: field(), fieldB: field({ label: 'label.b' }) },
  );
}

function node(container: HTMLElement, id: string): HTMLElement {
  const found = container.querySelector<HTMLElement>(`[data-node-id="${id}"]`);
  if (found === null) {
    throw new Error(`no rendered node with id '${id}'`);
  }
  return found;
}

describe('nested layout', () => {
  it('renders a grid container per region with one custom property per breakpoint', () => {
    const { container } = mount(render(nested()));
    const rendered = node(container, 'region-1');

    expect(rendered).toHaveAttribute('data-node-kind', 'REGION');
    expect(cssVariable(rendered, '--mp-cols-base')).toBe('1');
    expect(cssVariable(rendered, '--mp-cols-sm')).toBe('1');
    expect(cssVariable(rendered, '--mp-cols-md')).toBe('6');
    // lg is not configured; the cascade must carry md upward rather than reset to base.
    expect(cssVariable(rendered, '--mp-cols-lg')).toBe('6');
    expect(cssVariable(rendered, '--mp-cols-xl')).toBe('12');
  });

  it('maps the gap token onto a spacing token, never a length', () => {
    const { container } = mount(render(nested()));

    expect(cssVariable(node(container, 'region-1'), '--mp-grid-gap')).toBe('var(--mp-space-lg)');
  });

  it('makes a group both a grid cell of its parent and a grid container of its own', () => {
    const { container } = mount(render(nested()));
    const rendered = node(container, 'group-1');

    expect(rendered).toHaveAttribute('data-node-kind', 'GROUP');
    expect(cssVariable(rendered, '--mp-span-md')).toBe('6');
    expect(cssVariable(rendered, '--mp-span-xl')).toBe('8');

    const inner = rendered.querySelector<HTMLElement>('[data-grid-of="group-1"]');
    expect(inner).not.toBeNull();
    expect(cssVariable(inner!, '--mp-cols-md')).toBe('2');
    expect(cssVariable(inner!, '--mp-cols-xl')).toBe('2');
  });

  it('nests slots inside their group, not their region', () => {
    const { container } = mount(render(nested()));

    expect(node(container, 'group-1').contains(node(container, 'slot-1'))).toBe(true);
    expect(node(container, 'group-1').contains(node(container, 'slot-3'))).toBe(false);
  });

  it('turns a slot span into per-breakpoint custom properties', () => {
    const { container } = mount(render(nested()));

    expect(cssVariable(node(container, 'slot-2'), '--mp-span-base')).toBe('1');
    expect(cssVariable(node(container, 'slot-2'), '--mp-span-md')).toBe('2');
    expect(cssVariable(node(container, 'slot-2'), '--mp-span-xl')).toBe('2');
    expect(cssVariable(node(container, 'slot-3'), '--mp-span-xl')).toBe('4');
  });

  it('defaults an unconfigured span and grid to a single column', () => {
    const pm = model([region('region-1', [slot('slot-1', boundTo('fieldA'))], undefined)], {
      fieldA: field(),
    });
    const { container } = mount(render(pm));

    expect(cssVariable(node(container, 'region-1'), '--mp-cols-xl')).toBe('1');
    expect(cssVariable(node(container, 'slot-1'), '--mp-span-xl')).toBe('1');
  });

  it('renders a collapsible group as a disclosure that needs no JavaScript', () => {
    const pm = model(
      [
        region('region-1', [
          group('group-1', [slot('slot-1', boundTo('fieldA'))], {
            label: 'label.group1',
            collapsible: true,
          }),
        ]),
      ],
      { fieldA: field() },
    );
    const { container } = mount(render(pm));
    const rendered = node(container, 'group-1');

    expect(rendered.tagName).toBe('DETAILS');
    expect(rendered.querySelector('summary')?.textContent).toBe('label.group1');
  });
});
