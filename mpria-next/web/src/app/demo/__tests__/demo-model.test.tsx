import { render as mount } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import demoModel from '@/app/demo/demo-presentation-model.json';
import { parsePresentationModel } from '@/lib/pm/parse';
import { render } from '@/renderer/render';

/**
 * The checked-in model behind `/demo` is what makes the skeleton demonstrable with nothing else
 * running, so it is worth a test of its own — a fixture that silently stops parsing is a demo that
 * silently stops working.
 *
 * <p>Note what is *not* asserted: any field name. The model is configuration data and may say
 * whatever a customer says; this file may not repeat it.
 */
describe('the demo presentation model', () => {
  const pm = parsePresentationModel(demoModel);

  it('parses against the wire contract', () => {
    expect(pm.layout.regions.length).toBeGreaterThan(0);
  });

  it('has state for every field its layout binds, as the server invariant requires', () => {
    const { container } = mount(render(pm));

    expect(container.querySelector('[data-missing-field-id]')).toBeNull();
  });

  it('exercises the unregistered-component fallback exactly once, on purpose', () => {
    const { container } = mount(render(pm));

    expect(container.querySelectorAll('[data-unsupported-component-type]')).toHaveLength(1);
  });
});
