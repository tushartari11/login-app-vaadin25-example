import { cookies } from 'next/headers';

/**
 * Where the access token lives — on the server, and only on the server.
 *
 * <p>ARCHITECTURE.md §10 is explicit: **no tokens in `localStorage`**. Anything the browser's
 * JavaScript can read, a cross-site script can exfiltrate, and a token in `localStorage` is
 * readable by every script on the origin for its whole lifetime. Every module that needs a token
 * in this client is a server component or a route handler, and this is the only place that reads
 * one.
 *
 * <p>**Phase 1 target, and this is the whole gap.** Authorization Code + PKCE against Keycloak:
 * the callback route handler exchanges the code, stores the *refresh* token in an encrypted,
 * `HttpOnly`, `SameSite=Lax` cookie, keeps the short-lived access token server-side, and refreshes
 * it transparently. The browser never sees either. The two mechanisms below are the walking
 * skeleton's stand-in for that exchange — they change where the token comes from, and nothing
 * about where it is allowed to go.
 */

/** Dev-only. Set in a shell or `.env.local`; the natural fit for scripted and container runs. */
const TOKEN_ENV_VAR = 'MPRIA_DEV_ACCESS_TOKEN';

/**
 * Dev-only. Set it by hand in the browser's dev tools for interactive work. Not `HttpOnly` on
 * purpose — it is a developer convenience, and pretending otherwise would make it look like the
 * Phase 1 cookie it is not.
 */
const DEV_TOKEN_COOKIE = 'mpria_dev_access_token';

async function currentAccessToken(): Promise<string | undefined> {
  const fromEnvironment = process.env[TOKEN_ENV_VAR];
  if (fromEnvironment !== undefined && fromEnvironment !== '') {
    return fromEnvironment;
  }
  const jar = await cookies();
  return jar.get(DEV_TOKEN_COOKIE)?.value;
}

/** The `Authorization` header, or nothing at all when unauthenticated — never an empty bearer. */
export async function authorizationHeader(): Promise<Record<string, string>> {
  const token = await currentAccessToken();
  return token === undefined ? {} : { Authorization: `Bearer ${token}` };
}
