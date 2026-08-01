import type { Breakpoint, SpanMap } from '@/lib/pm/types';

/**
 * Ordered smallest to largest. The order is what makes {@link resolveSpan} a cascade, and it is the
 * same order as `com.zetcom.mpria.pm.Span.BREAKPOINTS` — these two implementations must agree.
 */
const BREAKPOINTS: readonly Breakpoint[] = ['base', 'sm', 'md', 'lg', 'xl'];

const DEFAULT_SPAN: SpanMap = { base: 1 };

function isBreakpoint(name: string): name is Breakpoint {
  return (BREAKPOINTS as readonly string[]).includes(name);
}

/**
 * Validates breakpoint names and span values and returns a copy ordered smallest to largest.
 *
 * <p>Mirrors `Span.normalise`. Rejecting rather than ignoring an unknown breakpoint matters: a
 * configuration that says `tablet` is a configuration whose author believes something the platform
 * will not do, and silently dropping it would ship that misunderstanding to end users.
 */
export function normaliseSpan(span: SpanMap | undefined): SpanMap {
  if (span === undefined || Object.keys(span).length === 0) {
    return { ...DEFAULT_SPAN };
  }

  const ordered: SpanMap = {};
  for (const breakpoint of BREAKPOINTS) {
    const value = span[breakpoint];
    if (value !== undefined) {
      if (!Number.isInteger(value) || value < 1) {
        throw new Error(
          `span for breakpoint '${breakpoint}' must be an integer >= 1 but was ${String(value)}`,
        );
      }
      ordered[breakpoint] = value;
    }
  }

  const unknown = Object.keys(span).filter((name) => !isBreakpoint(name));
  if (unknown.length > 0) {
    throw new Error(
      `unknown breakpoint(s): ${unknown.join(', ')}; expected any of ${BREAKPOINTS.join(', ')}`,
    );
  }

  return ordered;
}

/**
 * Resolves the effective value at `breakpoint` by cascading down from it to the largest defined
 * breakpoint at or below, falling back to a single column.
 *
 * <p>Mirrors `Span.resolve`.
 */
export function resolveSpan(span: SpanMap, breakpoint: Breakpoint): number {
  const index = BREAKPOINTS.indexOf(breakpoint);
  if (index < 0) {
    throw new Error(`unknown breakpoint '${String(breakpoint)}'`);
  }
  for (let i = index; i >= 0; i--) {
    const name = BREAKPOINTS[i];
    const value = name === undefined ? undefined : span[name];
    if (value !== undefined) {
      return value;
    }
  }
  return 1;
}

/**
 * Flattens a sparse, cascading {@link SpanMap} into one CSS custom property per breakpoint.
 *
 * <p>This is the whole reason the renderer never measures the window. The cascade is resolved
 * during render — on the server for the first paint — and the stylesheet's fixed set of media
 * queries simply reads the property that applies. Server-rendered HTML is therefore laid out
 * correctly before any JavaScript runs, and there is no hydration reflow.
 */
export function spanVariables(
  span: SpanMap,
  role: 'span' | 'cols',
): Record<`--mp-${string}`, string> {
  const variables: Record<string, string> = {};
  for (const breakpoint of BREAKPOINTS) {
    variables[`--mp-${role}-${breakpoint}`] = String(resolveSpan(span, breakpoint));
  }
  return variables;
}
