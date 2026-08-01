import { render as mount, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { render } from '@/renderer/render';

import { boundTo, field, model, region, slot } from './fixtures';

/**
 * `FieldState` is applied mechanically. The renderer is never told *why* a field is read-only —
 * lifecycle, permissions and configured expressions all arrive as the same four flags — so these
 * tests only ever assert the resulting DOM and ARIA.
 */
function oneField(state = field(), type = 'TEXT_FIELD') {
  return model([region('region-1', [slot('slot-1', boundTo('fieldA', type))])], { fieldA: state });
}

describe('required', () => {
  it('marks the control required for both the browser and assistive technology', () => {
    mount(render(oneField(field({ required: true }))));
    const control = screen.getByLabelText(/label\.a/);

    expect(control).toBeRequired();
    expect(control).toHaveAttribute('aria-required', 'true');
  });

  it('shows a marker that is hidden from assistive technology, since aria-required says it', () => {
    const { container } = mount(render(oneField(field({ required: true }))));

    expect(container.querySelector('[data-required-marker]')).toHaveAttribute('aria-hidden', 'true');
  });

  it('does not mark an optional field', () => {
    mount(render(oneField()));

    expect(screen.getByLabelText('label.a')).not.toBeRequired();
  });
});

describe('readOnly', () => {
  it('applies the native attribute and states the intent for assistive technology', () => {
    mount(render(oneField(field({ readOnly: true }))));
    const control = screen.getByLabelText('label.a');

    expect(control).toHaveAttribute('readonly');
    expect(control).toHaveAttribute('aria-readonly', 'true');
  });

  it('falls back to disabling controls HTML gives no readonly attribute', () => {
    // `select` and `checkbox` have no `readonly`. Disabling is the only native way to stop the
    // value changing, and aria-readonly records what was actually meant.
    mount(render(oneField(field({ type: 'BOOLEAN', readOnly: true }), 'CHECKBOX')));
    const control = screen.getByLabelText('label.a');

    expect(control).toBeDisabled();
    expect(control).toHaveAttribute('aria-readonly', 'true');
  });
});

describe('hidden', () => {
  it('removes the whole grid cell rather than leaving a hole in the layout', () => {
    const pm = model(
      [
        region('region-1', [
          slot('slot-1', boundTo('fieldA')),
          slot('slot-2', boundTo('fieldB')),
        ]),
      ],
      { fieldA: field({ hidden: true }), fieldB: field({ label: 'label.b' }) },
    );
    const { container } = mount(render(pm));

    expect(container.querySelector('[data-node-id="slot-1"]')).toBeNull();
    expect(container.querySelector('[data-node-id="slot-2"]')).not.toBeNull();
    expect(screen.queryByLabelText('label.a')).toBeNull();
  });
});

describe('errors', () => {
  it('marks the control invalid and points it at the messages', () => {
    mount(render(oneField(field({ errors: ['error.one', 'error.two'] }))));
    const control = screen.getByLabelText('label.a');

    expect(control).toHaveAttribute('aria-invalid', 'true');

    const describedBy = control.getAttribute('aria-describedby');
    expect(describedBy).not.toBeNull();

    const messages = document.getElementById(describedBy!);
    expect(messages).not.toBeNull();
    expect(messages).toHaveTextContent('error.one');
    expect(messages).toHaveTextContent('error.two');
  });

  it('leaves a valid control unmarked and undescribed', () => {
    mount(render(oneField()));
    const control = screen.getByLabelText('label.a');

    expect(control).not.toHaveAttribute('aria-invalid');
    expect(control).not.toHaveAttribute('aria-describedby');
  });
});

describe('label association', () => {
  it('uses a real label element bound to the control by id', () => {
    const { container } = mount(render(oneField()));
    const label = container.querySelector('label');
    const control = screen.getByLabelText('label.a');

    expect(label).not.toBeNull();
    expect(label!.getAttribute('for')).toBe(control.getAttribute('id'));
  });

  it('falls back to the field id when configuration supplies no label', () => {
    mount(render(oneField(field({ label: undefined }))));

    expect(screen.getByLabelText('fieldA')).toBeInTheDocument();
  });

  it('names the control after the field id so a native form submission is keyed correctly', () => {
    mount(render(oneField()));

    expect(screen.getByLabelText('label.a')).toHaveAttribute('name', 'fieldA');
  });
});
