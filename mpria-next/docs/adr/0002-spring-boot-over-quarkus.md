# ADR-0002 — Spring Boot 4 over Quarkus

* Status: Accepted
* Date: 2026-08-01

## Context

The services need a JVM application framework. The serious candidates were Spring Boot 4.1,
Quarkus 3.x, and Micronaut. The MVP ships **on-premises**, where memory footprint and startup time
are real constraints, which argues for Quarkus. The team's background is Java EE (EJB, CDI, JPA on
Payara), which is closer to Quarkus's programming model than to Spring's.

## Decision

Use **Spring Boot 4.1** with **Spring Modulith** inside each service.

## Rationale

* **Spring Modulith is the deciding factor.** It gives verified module boundaries, documented
  module dependencies, and an event-publication registry (transactional outbox) inside each
  service. That directly serves the design goal of being able to merge or re-cut service
  boundaries later without a rewrite. Quarkus has no equivalent.
* **Spring Authorization Server / Security 7** is the most complete OIDC resource-server story on
  the JVM, and OIDC is requirement #3.
* Contract tooling (springdoc, Pact JVM), Testcontainers integration (`@ServiceConnection`), and
  OTel autoconfiguration are all first-class and require no glue.
* Hiring and the available knowledge base are meaningfully deeper for Spring than for Quarkus.

## Rejected alternatives

* **Quarkus** — better cold start and RSS, and a CDI model the team already knows. Rejected
  because the modulith/outbox and OIDC advantages outweigh a footprint difference that a
  well-provisioned on-prem host absorbs. Revisit if on-prem hardware turns out to be constrained
  enough that per-service RSS becomes the binding limit.
* **Micronaut** — compile-time DI is attractive, ecosystem is the thinnest of the three.

## Consequences

* Per-service baseline RSS is roughly 250–400 MB. Budget for it in the on-prem sizing guide.
* If startup time becomes a problem (it should not — these are long-running services), Spring Boot
  AOT + GraalVM native image is available without changing frameworks.
* Java EE idioms do **not** transfer verbatim. `@Stateless` EJBs become `@Service` beans;
  container-managed transactions become `@Transactional`; `@Observes` becomes
  `ApplicationEventPublisher`. Team ramp-up is a real, one-time cost.
