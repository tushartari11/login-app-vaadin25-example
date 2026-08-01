import { NextResponse } from 'next/server';

import { authorizationHeader } from '@/lib/auth/access-token';
import { bffBaseUrl } from '@/lib/bff/client';

/**
 * Performs an action the presentation model offered.
 *
 * <p>It exists so the access token never reaches the browser (ARCHITECTURE.md §10): the client
 * posts the action it was *given* and this handler attaches credentials server-side.
 *
 * <p>It does not construct a URL. `href` and `method` come straight from `actions[]`, which the
 * BFF computed from lifecycle and permissions. What this handler does add is a check that `href`
 * is a relative path — otherwise a tampered or spoofed model could turn a route handler that holds
 * a valid bearer token into an open proxy pointed at any host on the network.
 *
 * <p>Known gap: `docs/CONTRACTS.md` fixes the BFF's write *paths* but not their request bodies.
 * This sends `{"values": {fieldId: value}}` and echoes `meta.etag` as `If-Match`, which rule 6 of
 * the assembly rules does specify. The body shape needs confirming against `presentation-bff`.
 */
export async function POST(request: Request): Promise<Response> {
  let invocation: ActionInvocation;
  try {
    invocation = parseInvocation(await request.json());
  } catch (error) {
    return NextResponse.json({ error: messageOf(error) }, { status: 400 });
  }

  const response = await fetch(`${bffBaseUrl()}${invocation.href}`, {
    method: invocation.method,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(invocation.etag === undefined ? {} : { 'If-Match': invocation.etag }),
      ...(await authorizationHeader()),
    },
    body: JSON.stringify({ values: invocation.values }),
    cache: 'no-store',
  });

  // A validation failure is not an error here: the BFF answers 200 with the re-rendered model and
  // the messages attached to the right controls. Anything genuinely wrong is passed through with
  // its own status rather than being flattened into one.
  const payload: unknown = await response.json().catch(() => null);
  return NextResponse.json(payload, { status: response.status });
}

interface ActionInvocation {
  readonly method: string;
  readonly href: string;
  readonly etag?: string;
  readonly values: Record<string, string>;
}

const ALLOWED_METHODS: readonly string[] = ['POST', 'PUT', 'PATCH', 'DELETE'];

function parseInvocation(body: unknown): ActionInvocation {
  if (typeof body !== 'object' || body === null) {
    return fail('body must be a JSON document');
  }
  const raw = body as Record<string, unknown>;

  const method = typeof raw['method'] === 'string' ? raw['method'].toUpperCase() : '';
  if (!ALLOWED_METHODS.includes(method)) {
    return fail(`method '${method}' is not an action method`);
  }

  const href = raw['href'];
  // A leading `//` is a protocol-relative URL, so "starts with /" alone is not enough.
  if (typeof href !== 'string' || !href.startsWith('/') || href.startsWith('//')) {
    return fail('href must be a path relative to the BFF, as supplied in actions[]');
  }

  const etag = typeof raw['etag'] === 'string' ? raw['etag'] : undefined;

  const values: Record<string, string> = {};
  const submitted = raw['values'];
  if (typeof submitted === 'object' && submitted !== null) {
    for (const [name, value] of Object.entries(submitted as Record<string, unknown>)) {
      if (typeof value === 'string') {
        values[name] = value;
      }
    }
  }

  return { method, href, etag, values };
}

function fail(message: string): never {
  throw new Error(message);
}

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
