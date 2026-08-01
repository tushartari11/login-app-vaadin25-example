# ADR-0003 — Server-side presentation model emitted as JSON

* Status: Accepted
* Date: 2026-08-01

## Context

Two requirements are in tension. The application must be **independent of any frontend framework**
(today Next.js, possibly something else later), and it must keep the **View ← PresentationModel ←
ApplicationModel ← Repository** separation that MpRIA v8 has.

A conventional SPA resolves this by exposing domain REST endpoints and re-implementing the
presentation model in TypeScript. That satisfies "no Vaadin" but discards the PM layer, and it
makes the admin-configurable layout (requirement #8) a client-side interpreter problem.

## Decision

The presentation model stays in **Java**, in `presentation-bff`, and is serialised to **JSON**.
The client is a **generic renderer** that maps `field.type` to a component and draws whatever tree
it is given.

## Rationale

* Framework independence is achieved by the *contract*, not by moving the layer. JSON is the most
  framework-agnostic thing available; a React renderer and a SvelteKit renderer consume the same
  document.
* Rules deciding whether a field is required, read-only, or hidden are domain rules composed from
  configuration, record lifecycle, and ACL. Keeping them server-side means one implementation.
* Admin layout changes take effect on the next request, with no client deployment. This is the
  requirement that a client-side PM cannot satisfy cleanly.
* Authorisation cannot drift: `actions[].enabled` is computed server-side. The client physically
  does not know how to construct a forbidden request.

## Rejected alternatives

* **Client-side PM in TypeScript** — more idiomatic React, better offline/optimistic UX. Rejected:
  duplicates domain rules and pushes layout interpretation into the client.
* **GraphQL** — solves over-fetching, not layout. The client would still have to know what a screen
  looks like. Orthogonal; could be added later as a query transport under the BFF.

## Consequences

* The BFF is on the critical path of every screen. It must be stateless, horizontally scalable,
  and it must cache configuration aggressively (safe: revisions are immutable).
* Highly interactive, non-form UI (a media lightbox, a timeline, a map) is a poor fit for a
  declarative PM. Such screens are hand-written React that calls domain APIs directly. The PM
  covers configurable forms and lists — the 80 % that is the product's actual surface.
* PM JSON types are **generated** into TypeScript from the backend JSON Schema. A CI drift test
  makes a breaking PM change fail the backend build.
