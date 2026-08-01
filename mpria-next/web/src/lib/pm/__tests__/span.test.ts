import { describe, expect, it } from 'vitest';

import { normaliseSpan, resolveSpan, spanVariables } from '@/lib/pm/span';
import type { SpanMap } from '@/lib/pm/types';

// These are deliberately the same cases as the Java `SpanTest` in `services/pm-contract`. The
// cascade is implemented twice — once server-side, once here — so it has to be tested twice with
// the same table, or the two implementations drift and configured layouts render differently
// depending on who resolved them.
describe('normaliseSpan', () => {
  it('defaults to a single column so an unconfigured layout is mobile-correct', () => {
    expect(normaliseSpan(undefined)).toEqual({ base: 1 });
    expect(normaliseSpan({})).toEqual({ base: 1 });
  });

  it('orders breakpoints smallest to largest so serialised layouts are diffable', () => {
    const unordered: SpanMap = { xl: 12, base: 1, md: 6 };

    // Compared as serialised JSON because key order is the property under test.
    expect(JSON.stringify(normaliseSpan(unordered))).toBe('{"base":1,"md":6,"xl":12}');
  });

  it('rejects unknown breakpoint names', () => {
    expect(() => normaliseSpan({ tablet: 4 } as unknown as SpanMap)).toThrowError(
      /tablet.*base, sm, md, lg, xl/s,
    );
  });

  it('rejects a span smaller than one column', () => {
    expect(() => normaliseSpan({ md: 0 })).toThrowError(/md.*>= 1/s);
  });

  it('returns a copy that later mutation of the input cannot reach', () => {
    const mutable: SpanMap = { base: 1 };
    const normalised = normaliseSpan(mutable);
    mutable.md = 6;

    expect(normalised).not.toHaveProperty('md');
  });
});

describe('resolveSpan cascades upward from the nearest smaller breakpoint', () => {
  const span = normaliseSpan({ base: 1, md: 6 });

  it.each([
    ['base', 1],
    ['sm', 1],
    ['md', 6],
    ['lg', 6],
    ['xl', 6],
  ] as const)('%s resolves to %i', (breakpoint, expected) => {
    expect(resolveSpan(span, breakpoint)).toBe(expected);
  });

  it('falls back to one column when nothing is defined below', () => {
    expect(resolveSpan({ xl: 12 }, 'base')).toBe(1);
  });

  it('rejects an unknown breakpoint', () => {
    expect(() => resolveSpan(span, 'phone' as never)).toThrowError(/phone/);
  });
});

describe('spanVariables', () => {
  it('flattens the cascade into one custom property per breakpoint', () => {
    // The cascade is resolved here, at render time, and never in the browser from a window size.
    // Emitting every breakpoint explicitly is what lets a handful of static media queries pick the
    // right value with no JavaScript, so server-rendered HTML is already laid out correctly.
    expect(spanVariables({ base: 1, md: 6 }, 'span')).toEqual({
      '--mp-span-base': '1',
      '--mp-span-sm': '1',
      '--mp-span-md': '6',
      '--mp-span-lg': '6',
      '--mp-span-xl': '6',
    });
  });

  it('emits a property for every breakpoint the stylesheet knows about, in cascade order', () => {
    expect(JSON.stringify(spanVariables({ base: 2 }, 'cols'))).toBe(
      '{"--mp-cols-base":"2","--mp-cols-sm":"2","--mp-cols-md":"2","--mp-cols-lg":"2","--mp-cols-xl":"2"}',
    );
  });
});
