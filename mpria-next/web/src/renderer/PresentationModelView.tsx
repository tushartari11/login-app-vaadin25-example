'use client';

import { useCallback, useRef, useState, type ReactElement } from 'react';

import type { ActionModel, PresentationModel } from '@/lib/pm/types';
import { ActionBar } from '@/renderer/ActionBar';
import { LayoutNodeView } from '@/renderer/LayoutNodeView';
import { DEFAULT_COMPONENT_REGISTRY, type ComponentRegistry } from '@/renderer/registry';
import styles from '@/renderer/renderer.module.css';

/** What an action carries back to whoever wired it up: the server's action, and the edited values. */
export interface ActionRequest {
  readonly action: ActionModel;
  readonly values: Readonly<Record<string, string>>;
}

export interface RenderOptions {
  /** Defaults to {@link DEFAULT_COMPONENT_REGISTRY}. Supply a superset to add field types. */
  readonly registry?: ComponentRegistry;
  /**
   * Invoked with the server's own `href` and `method`. The renderer performs no request itself —
   * transport, credentials and what to do with the re-rendered model are the host application's
   * concern, and keeping them out of here is what makes the renderer testable without a network.
   */
  readonly onAction?: (request: ActionRequest) => void | Promise<void>;
}

interface Props extends RenderOptions {
  readonly pm: PresentationModel;
}

export function PresentationModelView({
  pm,
  registry = DEFAULT_COMPONENT_REGISTRY,
  onAction,
}: Props): ReactElement {
  const formRef = useRef<HTMLFormElement>(null);
  const [status, setStatus] = useState('');

  const invoke = useCallback(
    (action: ActionModel) => {
      const request: ActionRequest = { action, values: collectValues(formRef.current) };
      setStatus(`${action.label}…`);
      void Promise.resolve()
        .then(() => onAction?.(request))
        .then(
          () => setStatus(`${action.label} ✓`),
          (error: unknown) => setStatus(`${action.label} — ${messageOf(error)}`),
        );
    },
    [onAction],
  );

  return (
    <form
      ref={formRef}
      className={styles.screen}
      data-screen-id={pm.meta.screenId}
      data-revision-id={pm.meta.revisionId}
      onSubmit={(event) => event.preventDefault()}
      // Validation is a domain rule evaluated server-side and returned as FieldState.errors.
      // Letting the browser pre-empt it would enforce a second, weaker, duplicated ruleset.
      noValidate
    >
      {pm.layout.regions.map((node) => (
        <LayoutNodeView key={node.id} node={node} fields={pm.fields} registry={registry} />
      ))}
      <footer className={styles.footer}>
        <ActionBar actions={pm.actions} onInvoke={invoke} />
        <p className={styles.status} role="status">
          {status}
        </p>
      </footer>
    </form>
  );
}

/**
 * Reads the current control values straight out of the form.
 *
 * <p>Uncontrolled inputs mean there is no second copy of the model in React state that could
 * disagree with the server's. An unchecked checkbox is simply absent, which is how HTML forms have
 * always behaved and what the BFF's partial-update semantics expect.
 */
function collectValues(form: HTMLFormElement | null): Record<string, string> {
  const values: Record<string, string> = {};
  if (form === null) {
    return values;
  }
  for (const [name, value] of new FormData(form).entries()) {
    if (typeof value === 'string') {
      values[name] = value;
    }
  }
  return values;
}

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
