import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

/**
 * Guards the two structural promises of the layout implementation.
 *
 * <p>First: the layout is never computed in JavaScript from a window size. A renderer that
 * measures the viewport produces server-rendered HTML with the wrong layout and then reflows on
 * hydration, which is both a visible flash and a wrong first paint for anything without
 * JavaScript. The cascade is resolved into custom properties at render time instead, and a fixed
 * set of media queries picks the value that applies.
 *
 * <p>Second: component internals respond to their *container*, not the viewport
 * (ARCHITECTURE.md §12). A field in a narrow side panel must stack even when the window is wide.
 */

// Resolved from a plain path rather than a `URL` instance: under the jsdom environment the global
// `URL` is jsdom's, which `fileURLToPath` will not accept.
const HERE = dirname(fileURLToPath(import.meta.url));
const SOURCE_ROOT = resolve(HERE, '../../');
const STYLESHEET = readFileSync(resolve(HERE, '../renderer.module.css'), 'utf8');

const MEASURING_APIS = [
  'innerWidth',
  'outerWidth',
  'matchMedia',
  'ResizeObserver',
  'getBoundingClientRect',
  'useLayoutEffect',
];

function rendererSources(): string[] {
  return readdirSync(SOURCE_ROOT, { recursive: true, encoding: 'utf8' })
    .filter((entry) => /\.tsx?$/.test(entry))
    .filter((entry) => !entry.includes('__tests__'))
    .filter((entry) => entry.startsWith('renderer/') || entry.startsWith('lib/pm/'));
}

describe('layout is CSS, not JavaScript', () => {
  it('finds the sources it is supposed to police', () => {
    expect(rendererSources().length).toBeGreaterThan(4);
  });

  it.each(rendererSources())('%s measures nothing', (source) => {
    const text = readFileSync(join(SOURCE_ROOT, source), 'utf8');
    const offending = MEASURING_APIS.filter((api) => text.includes(api));

    expect(offending, `${source} decides layout in JavaScript via: ${offending.join(', ')}`).toEqual(
      [],
    );
  });
});

describe('the stylesheet', () => {
  it.each(['40rem', '48rem', '64rem', '80rem'])(
    'has one media query at %s, matching the named breakpoints',
    (width) => {
      expect(STYLESHEET).toContain(`@media (min-width: ${width})`);
    },
  );

  it('keeps the number of viewport media queries small', () => {
    // Four: sm, md, lg, xl. `base` needs none, because it is the unconditional rule.
    expect(STYLESHEET.match(/@media \(min-width:/g)).toHaveLength(4);
  });

  it('uses container queries for component internals', () => {
    expect(STYLESHEET).toMatch(/container-type:\s*inline-size/);
    expect(STYLESHEET).toMatch(/@container\s+mp-cell/);
  });

  it('builds grids from repeat(N, minmax(0, 1fr)), never from fixed tracks', () => {
    expect(STYLESHEET).toContain('repeat(var(--mp-cols-base), minmax(0, 1fr))');
  });

  it('never positions anything absolutely — the v8 raster layout is what this replaces', () => {
    expect(STYLESHEET).not.toMatch(/position:\s*absolute/);
  });
});
