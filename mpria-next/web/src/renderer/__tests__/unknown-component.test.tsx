import { render as mount, screen } from '@testing-library/react';
import type { ReactElement } from 'react';
import { describe, expect, it } from 'vitest';

import { DEFAULT_COMPONENT_REGISTRY, type FieldComponentProps } from '@/renderer/registry';
import { render } from '@/renderer/render';

import { boundTo, field, model, region, slot } from './fixtures';

/**
 * Configuration is published independently of the client, so a published layout will eventually
 * name a component that a deployed renderer does not have. That must degrade visibly, not explode:
 * the rest of the screen stays usable and the message names the type so the gap is diagnosable
 * from a screenshot.
 */
describe('an unregistered component type', () => {
  const pm = model(
    [
      region('region-1', [
        slot('slot-1', boundTo('fieldA', 'NOT_IN_THIS_CLIENT')),
        slot('slot-2', boundTo('fieldB', 'TEXT_FIELD')),
      ]),
    ],
    { fieldA: field(), fieldB: field({ label: 'label.b' }) },
  );

  it('does not throw', () => {
    expect(() => mount(render(pm))).not.toThrow();
  });

  it('renders a visible fallback naming the missing type', () => {
    mount(render(pm));

    expect(screen.getByText(/NOT_IN_THIS_CLIENT/)).toBeVisible();
  });

  it('leaves every other slot on the screen working', () => {
    mount(render(pm));

    expect(screen.getByLabelText('label.b')).toBeInTheDocument();
  });
});

describe('the registry', () => {
  it('accepts a component for a type the built-in set does not cover', () => {
    // Adding a field type is registering a component. That is the whole extension mechanism, and
    // it is the property the entire server-driven design rests on.
    function CustomControl({ component }: FieldComponentProps): ReactElement {
      return <p>drawn by an extension: {component.type}</p>;
    }

    const pm = model([region('region-1', [slot('slot-1', boundTo('fieldA', 'TYPE_X'))])], {
      fieldA: field(),
    });

    mount(render(pm, { registry: { ...DEFAULT_COMPONENT_REGISTRY, TYPE_X: CustomControl } }));

    expect(screen.getByText(/drawn by an extension: TYPE_X/)).toBeInTheDocument();
  });
});
