'use client';

import { useRouter } from 'next/navigation';
import { useCallback, type ReactElement } from 'react';

import type { PresentationModel } from '@/lib/pm/types';
import { render } from '@/renderer/render';
import type { ActionRequest } from '@/renderer/render';

/**
 * Wires the renderer to the network.
 *
 * <p>Kept apart from the renderer on purpose: the renderer takes a model and returns a tree, and
 * knows nothing about `fetch`, cookies or routing. That separation is why the whole renderer test
 * suite runs without a server, and why replacing this transport does not touch a single component.
 */
export function ScreenView({ pm }: { pm: PresentationModel }): ReactElement {
  const router = useRouter();

  const onAction = useCallback(
    async ({ action, values }: ActionRequest) => {
      const response = await fetch('/api/actions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        // The server's own method and href, handed straight back. Rule 6 of the assembly rules:
        // the etag from meta is echoed as If-Match so a stale write is rejected, not merged.
        body: JSON.stringify({
          method: action.method,
          href: action.href,
          etag: pm.meta.etag,
          values,
        }),
      });

      if (!response.ok) {
        throw new Error(`${action.id} failed with ${response.status}`);
      }

      // The response *is* the re-rendered model, including any validation messages. Re-fetching
      // the server component is the simplest correct way to adopt it without keeping a second
      // copy of the model in client state that could disagree with the server's.
      router.refresh();
    },
    [pm.meta.etag, router],
  );

  return <>{render(pm, { onAction })}</>;
}
