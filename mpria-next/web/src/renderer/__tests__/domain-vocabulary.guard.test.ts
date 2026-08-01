import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

/**
 * The guard from `docs/ui-configuration.md` §7, made executable.
 *
 * <p>The renderer's whole value is that it knows nothing about museums, objects or inventory
 * numbers — it draws whatever tree the presentation model hands it. That property erodes silently:
 * nobody sets out to hardcode a field name, but a test written against a realistic screen quietly
 * bakes one in, and the next test copies it. Once the test suite knows the domain, the renderer is
 * free to know it too and nothing fails.
 *
 * <p>So the test suite is the thing that gets policed. Every renderer test uses meaningless
 * identifiers — `fieldA`, `typeX`, `screen-1` — and this test fails the build if a real one
 * appears. Customer vocabulary is allowed in exactly two places, neither of them a test: the
 * checked-in demo presentation model, which *is* configuration data, and a real configuration.
 */

// Resolved from a plain path rather than a `URL` instance: under the jsdom environment the global
// `URL` is jsdom's, which `fileURLToPath` will not accept.
const SELF_PATH = fileURLToPath(import.meta.url);
const SOURCE_ROOT = resolve(dirname(SELF_PATH), '../../');

/** This file necessarily contains every banned word, so it excludes itself. */
const SELF = relative(SOURCE_ROOT, SELF_PATH);

/**
 * Customer vocabulary, from the examples in `docs/CONTRACTS.md` and `docs/ui-configuration.md`
 * plus the terms ARCHITECTURE.md §1.3 names explicitly. Matched case-insensitively on whole words,
 * so `Object.keys` would trip this as readily as `"object"` — which is intentional, because a
 * guard with exceptions is a guard people learn to argue with.
 */
const DOMAIN_VOCABULARY = [
  'accession',
  'acquiredOn',
  'acquisition',
  'artist',
  'artwork',
  'condition',
  'creditLine',
  'curator',
  'description',
  'exhibition',
  'inventory',
  'inventoryNumber',
  'museum',
  'object',
  'owner',
  'painting',
  'provenance',
  'specimen',
  'title',
];

function testSources(): string[] {
  return readdirSync(SOURCE_ROOT, { recursive: true, encoding: 'utf8' })
    .filter((entry) => /\.tsx?$/.test(entry))
    .filter((entry) => entry.includes('__tests__') || /\.test\.tsx?$/.test(entry))
    .filter((entry) => entry !== SELF);
}

describe('no renderer test may speak the customer\'s vocabulary', () => {
  it('finds the test sources it is supposed to police', () => {
    // Without this, a broken glob would make the guard pass by scanning nothing at all — the
    // failure mode that makes a guard worse than no guard.
    expect(testSources().length).toBeGreaterThan(4);
  });

  it.each(testSources())('%s', (source) => {
    const text = readFileSync(join(SOURCE_ROOT, source), 'utf8');
    const offending = DOMAIN_VOCABULARY.filter((word) =>
      new RegExp(`\\b${word}\\b`, 'i').test(text),
    );

    expect(offending, `${source} names domain vocabulary: ${offending.join(', ')}`).toEqual([]);
  });
});
