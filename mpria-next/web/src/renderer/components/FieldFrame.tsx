import type { ReactElement, ReactNode } from 'react';

import type { FieldState } from '@/lib/pm/types';
import styles from '@/renderer/renderer.module.css';

/**
 * The attributes every control receives, derived mechanically from {@link FieldState}.
 *
 * <p>ARIA values are the strings `'true'` rather than booleans so that the absent case is genuinely
 * an absent attribute: `aria-invalid="false"` on a valid field is noise that some screen readers
 * announce.
 */
export interface ControlAttributes {
  readonly id: string;
  readonly name: string;
  readonly required: boolean;
  readonly 'aria-required'?: 'true';
  readonly 'aria-invalid'?: 'true';
  readonly 'aria-readonly'?: 'true';
  readonly 'aria-describedby'?: string;
}

interface FieldFrameProps {
  readonly slotId: string;
  readonly fieldId: string;
  readonly field: FieldState;
  /** Receives the wired-up attributes, so a control cannot forget to apply them. */
  readonly children: (control: ControlAttributes) => ReactNode;
}

/**
 * Label, control and messages for one field.
 *
 * <p>The accessibility wiring lives here and nowhere else. A registered component supplies markup
 * for the control and spreads what it is given; it cannot get `for`/`id` association or
 * `aria-describedby` wrong, because it never writes them.
 *
 * <p>Element ids are derived from the slot id, which configuration guarantees unique within a
 * screen. That keeps them identical on the server and in the browser, so nothing rewrites the DOM
 * on hydration.
 */
export function FieldFrame({ slotId, fieldId, field, children }: FieldFrameProps): ReactElement {
  const controlId = `mp-${slotId}-control`;
  const errorsId = `mp-${slotId}-errors`;
  const invalid = field.errors.length > 0;

  const control: ControlAttributes = {
    id: controlId,
    // The field id, so a native form submission is already keyed the way the server expects.
    name: fieldId,
    required: field.required,
    ...(field.required ? { 'aria-required': 'true' as const } : {}),
    ...(invalid ? { 'aria-invalid': 'true' as const, 'aria-describedby': errorsId } : {}),
    ...(field.readOnly ? { 'aria-readonly': 'true' as const } : {}),
  };

  return (
    <div className={styles.field} data-field-id={fieldId}>
      <label className={styles.fieldLabel} htmlFor={controlId}>
        {field.label ?? fieldId}
        {field.required ? (
          // aria-required already carries this; the glyph would only be repeated noise.
          <span className={styles.requiredMark} data-required-marker="" aria-hidden="true">
            *
          </span>
        ) : null}
      </label>
      <div className={styles.fieldControl}>
        {children(control)}
        {invalid ? (
          <ul className={styles.errors} id={errorsId}>
            {field.errors.map((message) => (
              <li key={message}>{message}</li>
            ))}
          </ul>
        ) : null}
      </div>
    </div>
  );
}
