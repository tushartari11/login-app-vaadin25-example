# ADR-0005 — Declarative configuration and typed extension points instead of Groovy custom code

* Status: Accepted
* Date: 2026-08-01

## Context

MpRIA v8 customises per customer through roughly 210 Groovy scripts (`MpCustomization`). Custom
code can intercept persistence and the event mechanism. The consequences observed in practice:

* Customer behaviour is invisible to the type system and to the framework's test suite.
* A framework refactoring can break a customer at runtime, in production, with no compile-time signal.
* Scripts run in-process with full access to repositories, so there is no security or resource boundary.
* The Groovy-Eclipse compiler's proceed-on-error mode produces classes that throw
  "Unresolved compilation problem" at runtime — a whole class of defects that only exists because
  of this layer.

## Decision

Remove in-process scripting entirely. Customisation is expressed, in order of preference:

1. **Declarative configuration** — field definitions, layouts, validation rules, vocabularies,
   lifecycle states. Covers the large majority of real customisations.
2. **A restricted expression language** for computed fields, conditional visibility, and validation
   predicates. Sandboxed, side-effect free, no I/O, no repository access, evaluation
   time- and depth-bounded.
3. **Out-of-process webhooks** for genuine business-process integration, invoked on domain events
   with a versioned payload, retried, and observable.

There is no fourth option. In particular, nothing customer-supplied may run inside a domain
service's JVM with access to a repository.

## Rationale

* Options 1 and 2 are data. They can be validated at publish time, diffed, exported, and tested
  without deploying code.
* Option 3 puts a process and network boundary around arbitrary customer logic: it can fail, be
  slow, or be malicious without corrupting a transaction.
* The extension surface becomes a **versioned contract** (the event payload), which is exactly the
  thing v8 lacks.

## Rejected alternatives

* **Keep Groovy, sandboxed** — SecurityManager is removed in modern Java and Groovy sandboxing is
  historically porous. Rejected.
* **WASM plugins** — genuinely interesting: real sandbox, real resource limits, language-agnostic.
  Rejected *for now* on ecosystem maturity for JVM hosts and on the operational cost of a plugin
  distribution mechanism. Reconsider if expression language + webhooks prove insufficient.

## Consequences

* Some v8 customisations will have no direct equivalent and must be re-expressed. Expect a
  discovery exercise per customer; expect a small number to require a framework feature rather
  than a configuration.
* The expression language must be designed carefully — it is a product surface, not an
  implementation detail. It needs documentation, an editor with autocomplete in the admin console,
  and a test harness so an SME can verify a rule before publishing.
* Webhook latency cannot be on a synchronous write path. Webhooks fire on committed events only.
