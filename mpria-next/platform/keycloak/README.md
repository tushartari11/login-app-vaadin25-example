# Development realm

`mpria-realm.json` is imported by the Keycloak container on startup
(`start-dev --import-realm`). **Development only** — a production realm is managed as
configuration, not imported from a file with plaintext passwords in it.

## Users

| Username | Password | Realm roles |
| --- | --- | --- |
| `curator` | `curator` | `mpria-user` |
| `configadmin` | `configadmin` | `mpria-user`, `mpria-config-admin`, `mpria-config-publisher` |
| `reader` | `reader` | `mpria-user` |

All three carry the same `tenant_id` (`1111…`) and `org_unit_id` (`3333…`), matching
`CatalogFixtures` so tokens and test data line up.

## Clients

| Client | Flow | Used by |
| --- | --- | --- |
| `mpria-web` | Authorization Code + PKCE | the operator client on :3000 |
| `mpria-admin-console` | Authorization Code + PKCE | the layout designer on :3002 |
| `mpria-dev-cli` | Direct access grants (password) | `curl` and smoke tests **only** |

`mpria-dev-cli` is the sole client with direct access grants enabled. Never create its equivalent
in a real deployment.

## The claims that matter

Each client carries two `oidc-usermodel-attribute-mapper` protocol mappers that copy the user
attributes `tenant_id` and `org_unit_id` into the access token. Every service derives the acting
tenant from those claims and from nothing else — a client cannot nominate the tenant it writes to
(see `JwtPrincipals` in catalog-service).

The mappers are duplicated across the three clients on purpose. The tidier-looking alternative — a
shared `mpria-tenant` client scope — is a trap; see below.

## Two import traps, both hit while building this

1. **An unknown top-level key fails the entire import.** Keycloak's `RealmRepresentation` is not
   lenient: a stray `"_comment"` aborts the import and the container exits 1. Notes belong in this
   README, not in the JSON.

2. **Declaring `clientScopes` replaces the realm's built-in scopes rather than adding to them.**
   A realm representation containing a `clientScopes` array is treated as the *complete* set for
   that realm, so `roles`, `profile`, `email`, `basic` and `web-origins` are never created. The
   symptom is subtle and easy to miss: tokens are issued successfully and carry your custom claims,
   but `realm_access` is `null` and `preferred_username` is absent — so every role check silently
   fails while authentication appears to work. Listing the built-in names under
   `defaultClientScopes` does not rescue it, because the scopes themselves do not exist.

   Hence: per-client `protocolMappers`, no custom client scope, and no `defaultClientScopes`
   override anywhere in this file.

## Editing this file

Verify a change actually imported:

```bash
docker compose up -d keycloak
curl -sf http://localhost:8180/realms/mpria/.well-known/openid-configuration > /dev/null && echo OK

curl -s -X POST http://localhost:8180/realms/mpria/protocol/openid-connect/token \
  -d grant_type=password -d client_id=mpria-dev-cli \
  -d username=curator -d password=curator | jq -r .access_token \
  | cut -d. -f2 | base64 -d 2>/dev/null | jq '{tenant_id, org_unit_id, realm_access}'
```
