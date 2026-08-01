import type { ReactElement } from 'react';

import type { ComponentRef } from '@/lib/pm/types';
import styles from '@/renderer/renderer.module.css';

/**
 * Drawn when the layout names a component this client does not have.
 *
 * <p>Configuration is published without deploying the client, so a configuration will outrun a
 * renderer sooner or later. The screen must stay usable and the message must name the type, so the
 * gap is diagnosable from a screenshot rather than from a stack trace nobody sent.
 */
export function UnsupportedComponent({ component }: { component: ComponentRef }): ReactElement {
  return (
    <p className={styles.fallback} data-unsupported-component-type={component.type}>
      No component is registered for “{component.type}”. Update this client, or correct the
      configuration.
    </p>
  );
}

/**
 * Drawn when a slot binds a field the model carries no state for.
 *
 * <p>The server's `PresentationModel` constructor makes this unreachable over the wire, so
 * reaching it means a hand-authored or partially migrated model — still worth rendering readably.
 */
export function MissingBinding({ component }: { component: ComponentRef }): ReactElement {
  return (
    <p className={styles.fallback} data-missing-field-id={component.fieldId ?? ''}>
      “{component.type}” is bound to “{component.fieldId ?? '(no field id)'}”, which this screen
      carries no state for.
    </p>
  );
}
