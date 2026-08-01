import { describe, expect, it } from 'vitest';

import { parsePresentationModel } from '@/lib/pm/parse';

function wireModel(): unknown {
  return {
    meta: { screenId: 'screen-1', revisionId: 'rev-1', recordId: 'rec-1', etag: '"0"' },
    layout: {
      screenId: 'screen-1',
      recordType: 'typeX',
      regions: [
        {
          kind: 'REGION',
          id: 'region-1',
          grid: { columns: { base: 1, md: 6 }, gap: 'md' },
          children: [
            {
              kind: 'SLOT',
              id: 'slot-1',
              span: { base: 1 },
              component: { type: 'TEXT_FIELD', fieldId: 'fieldA' },
            },
          ],
        },
      ],
    },
    fields: { fieldA: { type: 'STRING', label: 'label.a', value: 'v' } },
    actions: [{ id: 'act-1', label: 'label.act', method: 'PUT', href: '/x', enabled: true }],
  };
}

describe('parsePresentationModel', () => {
  it('narrows an unknown wire payload into the typed model', () => {
    const pm = parsePresentationModel(wireModel());

    expect(pm.meta.screenId).toBe('screen-1');
    expect(pm.layout.regions).toHaveLength(1);
    expect(pm.fields['fieldA']?.type).toBe('STRING');
    expect(pm.actions[0]?.href).toBe('/x');
  });

  it('supplies the defaults the server omits under NON_NULL serialisation', () => {
    const pm = parsePresentationModel(wireModel());
    const field = pm.fields['fieldA'];

    expect(field?.readOnly).toBe(false);
    expect(field?.required).toBe(false);
    expect(field?.hidden).toBe(false);
    expect(field?.errors).toEqual([]);
    expect(field?.options).toEqual([]);
    expect(pm.meta.capabilities).toEqual([]);
  });

  it('accepts a component type it has never heard of, because the fallback handles it', () => {
    const wire = wireModel() as { layout: { regions: { children: { component: unknown }[] }[] } };
    wire.layout.regions[0]!.children[0]!.component = { type: 'NOT_IN_THIS_CLIENT' };

    expect(() => parsePresentationModel(wire)).not.toThrow();
  });

  it('rejects a payload whose layout node kind is not part of the sealed hierarchy', () => {
    const wire = wireModel() as { layout: { regions: { children: { kind: string }[] }[] } };
    wire.layout.regions[0]!.children[0]!.kind = 'CAROUSEL';

    expect(() => parsePresentationModel(wire)).toThrowError(/CAROUSEL/);
  });

  it('rejects a payload that is not a JSON document at all', () => {
    expect(() => parsePresentationModel('nope')).toThrowError(/PresentationModel/);
  });

  it('rejects a disabled action with no reason, matching the server-side invariant', () => {
    const wire = wireModel() as { actions: { enabled: boolean }[] };
    wire.actions[0]!.enabled = false;

    expect(() => parsePresentationModel(wire)).toThrowError(/disabledReason/);
  });
});
