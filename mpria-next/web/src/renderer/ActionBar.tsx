import { useState, type ReactElement } from 'react';

import type { ActionModel } from '@/lib/pm/types';
import { classNames } from '@/renderer/grid';
import styles from '@/renderer/renderer.module.css';

interface ActionBarProps {
  readonly actions: readonly ActionModel[];
  readonly onInvoke: (action: ActionModel) => void;
}

/**
 * Renders `actions[]` and nothing else.
 *
 * <p>No URL is constructed here and no button's availability is decided here: `href`, `method` and
 * `enabled` are computed server-side from lifecycle and permissions. That is what removes the
 * whole class of client/server authorisation drift — a client that cannot invent an operation
 * cannot invent one it is not allowed to perform.
 */
export function ActionBar({ actions, onInvoke }: ActionBarProps): ReactElement | null {
  const [awaitingConfirmation, setAwaitingConfirmation] = useState<string | null>(null);

  if (actions.length === 0) {
    return null;
  }

  return (
    <div className={styles.actionBar} role="group" aria-label="Actions">
      {actions.map((action) => {
        if (!action.enabled) {
          return <DisabledAction key={action.id} action={action} />;
        }
        if (awaitingConfirmation === action.id) {
          return (
            <Confirmation
              key={action.id}
              action={action}
              onConfirm={() => {
                setAwaitingConfirmation(null);
                onInvoke(action);
              }}
              onCancel={() => setAwaitingConfirmation(null)}
            />
          );
        }
        return (
          <button
            key={action.id}
            type="button"
            className={classNames(styles.action, action.confirm && styles.actionPrimary)}
            onClick={() =>
              action.confirm ? setAwaitingConfirmation(action.id) : onInvoke(action)
            }
          >
            {action.label}
          </button>
        );
      })}
    </div>
  );
}

/**
 * `aria-disabled` rather than the `disabled` attribute, deliberately: a disabled button leaves the
 * tab order, so a keyboard user could never reach the explanation of why it is unavailable. The
 * click handler is what actually makes it inert.
 */
function DisabledAction({ action }: { action: ActionModel }): ReactElement {
  const reasonId = `mp-action-${action.id}-reason`;
  return (
    <div className={styles.actionItem}>
      <button
        type="button"
        className={styles.action}
        aria-disabled="true"
        aria-describedby={reasonId}
        onClick={(event) => event.preventDefault()}
      >
        {action.label}
      </button>
      <span className={styles.actionReason} id={reasonId}>
        {action.disabledReason}
      </span>
    </div>
  );
}

/**
 * An inline two-step confirmation rather than `window.confirm`, which cannot be styled, cannot be
 * made accessible, and blocks the event loop.
 */
function Confirmation({
  action,
  onConfirm,
  onCancel,
}: {
  action: ActionModel;
  onConfirm: () => void;
  onCancel: () => void;
}): ReactElement {
  return (
    <div
      className={classNames(styles.actionItem, styles.confirmItem)}
      role="group"
      aria-label={`Confirm ${action.label}`}
    >
      <button
        type="button"
        className={classNames(styles.action, styles.actionPrimary)}
        onClick={onConfirm}
        autoFocus
      >
        Confirm {action.label}
      </button>
      <button type="button" className={styles.action} onClick={onCancel}>
        Cancel
      </button>
    </div>
  );
}
