import { authorizationHeader } from '@/lib/auth/access-token';
import { parsePresentationModel } from '@/lib/pm/parse';
import type { PresentationModel } from '@/lib/pm/types';

/**
 * The only service this client talks to (ARCHITECTURE.md §3). Everything else — configuration,
 * catalogue, search, media — is behind it, which is what keeps the browser's dependency surface a
 * single contract rather than five.
 */
const DEFAULT_BFF_URL = 'http://localhost:8080';

export function bffBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_BFF_URL;
  return configured !== undefined && configured !== '' ? configured : DEFAULT_BFF_URL;
}

/** Raised when the BFF answers with a status this client cannot turn into a screen. */
class BffError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'BffError';
  }
}

/**
 * Fetches a screen. Server-side only — the access token must not cross into the browser.
 *
 * <p>`no-store`, because a presentation model folds in the principal's permissions and the
 * record's lifecycle. Caching one and serving it to the next request would be an authorisation
 * bug, not a performance win. The BFF caches the immutable half (configuration, by `revisionId`)
 * where it is actually safe to.
 */
export async function fetchScreen(
  screenId: string,
  recordId: string,
): Promise<PresentationModel> {
  const path = `/api/v1/screens/${encodeURIComponent(screenId)}/${encodeURIComponent(recordId)}`;
  const response = await fetch(`${bffBaseUrl()}${path}`, {
    headers: { Accept: 'application/json', ...(await authorizationHeader()) },
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new BffError(response.status, `GET ${path} returned ${response.status}`);
  }

  return parsePresentationModel(await response.json());
}
