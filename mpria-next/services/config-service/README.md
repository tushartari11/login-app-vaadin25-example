# config-service

The administration application, and the Phase-0 replacement for the ULC configuration tool. It
owns record types, field definitions, screen layouts and immutable configuration revisions.

Runs on **8082**. Starts and is fully usable with every other service down — and the reverse holds
too, which is the point: consumers resolve a published revision and cache it, so this service is
not on the runtime critical path and may be restarted, upgraded or firewalled to an internal
network without cataloguing stopping.

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
mvn -B -s .mvn/settings-central.xml -pl services/config-service test
```

## Storage: none, deliberately

**There is no database and no datasource dependency.** The published revision is seeded at startup
from the classpath resource [`config/seed-revision.json`](src/main/resources/config/seed-revision.json)
into an in-memory store behind the `ConfigurationRepository` port. Anything published or imported
at runtime lives until the process exits and no longer.

That is a scoping decision rather than an omission. What the walking skeleton has to demonstrate is
that a *correct* revision is served, cached and honoured by three other services; durability adds
no evidence for that and would add a container, a schema and a migration tool to every developer's
loop. **Phase 2** replaces `InMemoryConfigurationRepository` with this service's own PostgreSQL
schema and Flyway migrations, behind the same port and with no change to the application layer.

The seed is written in the **export bundle format**, so the same file could have been produced by
`GET /api/v1/admin/revisions/{revisionId}/export` and fed back through `POST /api/v1/admin/import`.
A malformed seed fails startup — a configuration service running with no configuration is worse
than one that is plainly down.

A cold start also opens **`draft-1`** from the seeded revision, because the Phase-0 contract has no
create-draft endpoint and the administration API would otherwise have nothing to act on.

## The publish gate

Validation is the reason this is a service and not a JSON file in a bucket. A published revision is
cached indefinitely by every consumer, so a configuration that would render a broken screen is
refused while the person who wrote it is still looking at it. Publishing rejects:

| Rejection | Message |
| --- | --- |
| A layout binding a field the record type does not define | `binds field 'provenance', which record type 'object' does not define` |
| A component type absent from the registry | `uses component type 'CAROUSEL', which is not in the component registry` |
| A component that cannot draw the field's type | `binds field 'acquiredOn' of type DATE to component 'TEXT_FIELD', which accepts [STRING]` |
| A field bound to a component that draws no field | `binds field 'title' to component 'MEDIA_GALLERY', which accepts no field` |
| A field-drawing component left unbound | `uses component 'TEXT_FIELD', which requires a field binding` |
| A screen for a record type the revision does not define | `references record type 'acquisition', which this revision does not define` |

Every problem is reported at once and each names the screen and the node, so the console can select
the offending slot on the canvas. An SME must be told which slot to fix, never handed a stack trace.

Editing a draft does **not** validate: a field dragged off the canvas before its replacement is
dragged on is a normal keystroke, not an error.

## Authorisation

Realm roles arrive from Keycloak under `realm_access.roles` — not under `scope`, where Spring's
default converter looks — so `RealmRoleAuthoritiesConverter` maps them to `ROLE_*` authorities.

| Access | Requires |
| --- | --- |
| Read the published configuration | any authenticated principal |
| Read or edit a draft, export, import | `mpria-config-admin` |
| Publish | `mpria-config-admin` **and** `mpria-config-publisher` |

Reading is not an administrative privilege: the operator client resolves a layout on every screen it
opens. Publishing needs the second role because editing a draft affects nobody, while publishing
changes what every operator in the tenant sees on their next screen.

Errors are RFC 9457 problem documents under `https://docs.mpria.zetcom.com/problems/`:
`record-type-not-found`, `screen-not-found`, `revision-not-found`, `draft-not-found` (404),
`malformed-configuration` (400), `stale-draft` (412) and `invalid-configuration` (422, carrying the
`errors` array above).

## Known Phase-0 gaps

* **Tenancy.** `X-Tenant-Id` is accepted and ignored; the store holds one configuration for the
  whole installation. Per-tenant revisions arrive with the database in Phase 2, at which point the
  tenant comes from the token's `tenant_id` claim and never from the header.
* **No `ConfigurationPublished` event.** Publication moves the pointer; consumers' caches are not
  invalidated. `catalog-service` already exposes an `evictAll()` for the day the event exists.
* **Bundles are JSON, not signed zips.** The manifest, content checksum and signature described in
  `docs/ui-configuration.md` §5.1 are Phase 2.
* **No overlay merge.** Base-plus-overlay configuration (§5) is not modelled yet.
