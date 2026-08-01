import type { ReactElement } from 'react';

import { asText, boundComponent, numberOption } from '@/renderer/components/bound';
import type { FieldComponentProps } from '@/renderer/registry';
import { classNames } from '@/renderer/grid';
import styles from '@/renderer/renderer.module.css';

/**
 * The built-in controls.
 *
 * <p>Not one of them knows what it is displaying. Each is chosen by `component.type` from the
 * layout and configured by `FieldState`, so the set below is complete for every screen the
 * platform will ever be configured to produce — and extending it is adding an entry, never editing
 * a screen.
 *
 * <p>Values are uncontrolled (`defaultValue`). The presentation model is the server's opinion of
 * the field at request time; the browser owns edits until an action sends them back, which keeps
 * the renderer free of a second, client-side copy of the model that could disagree with it.
 */

export const TextField = boundComponent('TextField', ({ field, control }) => (
  <input
    {...control}
    className={styles.control}
    type="text"
    readOnly={field.readOnly}
    defaultValue={asText(field.value)}
  />
));

export const RichText = boundComponent('RichText', ({ field, options, control }) => (
  <textarea
    {...control}
    className={styles.control}
    rows={numberOption(options, 'rows', 4)}
    readOnly={field.readOnly}
    defaultValue={asText(field.value)}
  />
));

export const NumberField = boundComponent('NumberField', ({ field, control }) => (
  <input
    {...control}
    className={styles.control}
    type="number"
    readOnly={field.readOnly}
    defaultValue={asText(field.value)}
  />
));

export const DateField = boundComponent('DateField', ({ field, control }) => (
  <input
    {...control}
    className={styles.control}
    type="date"
    readOnly={field.readOnly}
    defaultValue={asText(field.value)}
  />
));

export const Checkbox = boundComponent('Checkbox', ({ field, control }) => (
  // HTML gives a checkbox no `readonly`, so disabling is the only native way to stop the value
  // changing. `aria-readonly` on the control records what was actually meant.
  <input
    {...control}
    className={classNames(styles.control, styles.checkbox)}
    type="checkbox"
    value="true"
    disabled={field.readOnly}
    defaultChecked={field.value === true}
  />
));

export const Select = boundComponent('Select', ({ field, control }) => (
  <select
    {...control}
    className={styles.control}
    disabled={field.readOnly}
    defaultValue={asText(field.value)}
  >
    {/* Options come from FieldState, never from a list compiled into this client. */}
    {field.required ? null : <option value="" />}
    {field.options.map((option) => (
      <option key={option.value} value={option.value}>
        {option.label}
      </option>
    ))}
  </select>
));

/**
 * An unbound placeholder. Media is a Phase 5 context (ARCHITECTURE.md §15); the slot exists now so
 * that a configured layout containing one renders at its configured size rather than collapsing.
 */
export function MediaGallery({ component }: FieldComponentProps): ReactElement {
  return (
    <section className={styles.placeholder} role="group" aria-label="Media gallery">
      <p>Media gallery — no media service in the walking skeleton ({component.type}).</p>
    </section>
  );
}
