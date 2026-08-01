import { render as mount, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { render } from '@/renderer/render';

import { boundTo, field, model, option, region, slot } from './fixtures';

function withComponent(type: string, state = field(), options?: Record<string, unknown>) {
  return model([region('region-1', [slot('slot-1', boundTo('fieldA', type, options))])], {
    fieldA: state,
  });
}

describe('the built-in component registry', () => {
  it('renders TEXT_FIELD as a single-line text input', () => {
    mount(render(withComponent('TEXT_FIELD', field({ value: 'v1' }))));
    const control = screen.getByLabelText('label.a');

    expect(control.tagName).toBe('INPUT');
    expect(control).toHaveAttribute('type', 'text');
    expect(control).toHaveValue('v1');
  });

  it('renders RICH_TEXT as a textarea whose row count comes from component options', () => {
    mount(render(withComponent('RICH_TEXT', field({ value: 'v1' }), { rows: 6 })));
    const control = screen.getByLabelText('label.a');

    expect(control.tagName).toBe('TEXTAREA');
    expect(control).toHaveAttribute('rows', '6');
  });

  it('renders NUMBER_FIELD as a numeric input', () => {
    mount(render(withComponent('NUMBER_FIELD', field({ type: 'INTEGER', value: 42 }))));
    const control = screen.getByLabelText('label.a');

    expect(control).toHaveAttribute('type', 'number');
    expect(control).toHaveValue(42);
  });

  it('renders DATE_FIELD as a date input', () => {
    mount(render(withComponent('DATE_FIELD', field({ type: 'DATE', value: '2026-08-01' }))));
    const control = screen.getByLabelText('label.a');

    expect(control).toHaveAttribute('type', 'date');
    expect(control).toHaveValue('2026-08-01');
  });

  it('renders CHECKBOX from a boolean value', () => {
    mount(render(withComponent('CHECKBOX', field({ type: 'BOOLEAN', value: true }))));

    expect(screen.getByLabelText('label.a')).toBeChecked();
  });

  it('renders SELECT from FieldState.options, never from a hardcoded list', () => {
    const state = field({
      type: 'ENUM',
      value: 'v2',
      options: [option('v1', 'label.v1'), option('v2', 'label.v2')],
    });
    mount(render(withComponent('SELECT', state)));
    const control = screen.getByLabelText('label.a');

    expect(control.tagName).toBe('SELECT');
    expect(control).toHaveValue('v2');
    expect(screen.getAllByRole('option').map((each) => each.textContent)).toEqual([
      '',
      'label.v1',
      'label.v2',
    ]);
  });

  it('omits the empty choice from a required SELECT', () => {
    const state = field({ type: 'ENUM', required: true, value: 'v1', options: [option('v1')] });
    mount(render(withComponent('SELECT', state)));

    expect(screen.getAllByRole('option')).toHaveLength(1);
  });

  it('renders MEDIA_GALLERY as an unbound placeholder with no field state at all', () => {
    const pm = model([region('region-1', [slot('slot-1', { type: 'MEDIA_GALLERY' })])]);
    const { container } = mount(render(pm));

    expect(container.querySelector('[data-component-type="MEDIA_GALLERY"]')).not.toBeNull();
    expect(screen.getByRole('group', { name: /gallery/i })).toBeInTheDocument();
  });

  it('degrades a bound component whose field state is missing instead of throwing', () => {
    // The server's PresentationModel constructor makes this impossible, so reaching it means a
    // hand-authored or partially migrated model — which should still be readable, not a blank page.
    const pm = model([region('region-1', [slot('slot-1', boundTo('fieldZ'))])]);

    expect(() => mount(render(pm))).not.toThrow();
    expect(screen.getByText(/fieldZ/)).toBeInTheDocument();
  });
});
