---
name: adr-write
description: >
  Use this skill when recording an architecture decision for MpRIA Next, or when revisiting one.
  Triggers on: "write an ADR", "record this decision", "we decided to use X", "document why we
  chose", "add a decision record", "supersede ADR-000N", "revisit the graph database decision", or
  any moment where a choice was made between real alternatives and the reasoning would otherwise be
  lost. Also use it when a change contradicts an existing ADR.
---

# Write an Architecture Decision Record

`docs/adr/` holds the reasoning behind this project's shape. The value is **not** in recording what
was chosen — the code already says that. It is in recording **what was rejected and why**, and
**what would make us change our minds**.

Those two sections are the first to be dropped when writing in a hurry, and they are the only
reason anyone reads an ADR a year later.

---

## When an ADR is warranted

Write one when **all three** hold:

1. A real alternative existed and was seriously considered
2. Reversing the decision later would be expensive
3. A competent engineer might otherwise reasonably assume the opposite

Do **not** write one for: a decision with no alternative, a coding convention (those belong in
`CLAUDE.md` or a checklist), or something already fully explained by `ARCHITECTURE.md`.

If a change **contradicts** an existing ADR, do not quietly diverge. Either amend that ADR or
supersede it — and say which.

---

## Format

Filename: `docs/adr/000N-kebab-case-title.md`, numbered sequentially. Never renumber an existing
one; other documents link to them.

```markdown
# ADR-000N — <decision, stated as an outcome, not a question>

* Status: Accepted | Superseded by ADR-000M | Revisited YYYY-MM-DD
* Date: YYYY-MM-DD

## Context

The forces at play. What is true about the problem, the team, the constraints and the deployment
model that make this a real choice. Include the uncomfortable facts — team size, on-prem
requirement, existing skills, what the v8 codebase taught us. If a requirement drove it, name it.

## Decision

What we are doing, stated plainly and specifically enough to act on. Include the concrete artefacts
where useful — the dependency, the annotation, the table shape.

## Rationale

Why this, in terms of the forces above. Prefer concrete consequences over adjectives: not "cleaner"
but "a developer can `SELECT *` and understand it".

## Rejected alternatives

* **<Alternative>** — its genuine strengths, stated fairly. Then why it lost, in terms of *this*
  project's constraints rather than in the abstract.

State the strengths honestly. An ADR that strawmans the alternatives is worthless, because the
alternative will be proposed again by someone who sees those strengths.

## Consequences

What this costs, what it makes harder, and what must now be true. Include the mitigations that are
actually implemented, not aspirations.

## Revisit trigger  (required when the decision defers something)

The measurable condition under which this should be reopened. "When it becomes a problem" is not a
trigger. "Any traversal exceeding four hops, or a query the CTE cannot serve under 200 ms p95" is.
```

---

## Quality bar — check each before finishing

- [ ] The title states an **outcome** ("Spring Boot 4 over Quarkus"), not a topic ("Framework choice")
- [ ] Context names the **real constraints**, including the awkward ones
- [ ] At least one rejected alternative, with its strengths stated fairly
- [ ] Rationale reasons from *this* project's forces, not from general best practice
- [ ] Consequences include the **costs**, not only the benefits
- [ ] A revisit trigger, measurable, whenever something is deferred
- [ ] Cross-references to the ADRs it interacts with
- [ ] British-English spelling, matching the existing set

---

## House conventions, from the existing seven

Read one or two before writing — `0004-jsonb-over-eav.md` and `0006-defer-graph-database.md` are
the best models.

- **Be candid about cost.** ADR-0007 records that microservices were chosen against the usual
  advice, names the price, and documents the hedge (Modulith cores) that makes it survivable. An
  ADR may disagree with its own decision's downsides.
- **Deferral is a decision worth recording.** ADR-0006 defers a graph database and states the exact
  conditions for revisiting, plus the preferred escalation order.
- **Name what the decision forecloses.** ADR-0005 removes Groovy and states plainly that some v8
  customisations will have no direct equivalent.
- **Point at the enforcement.** If a rule is machine-checked (ArchUnit, Maven Enforcer, a lint
  rule), say which — a decision with a guard is worth more than one with a paragraph.

---

## After writing

- [ ] Add the row to the ADR table in `ARCHITECTURE.md` §16
- [ ] Link it from any section it explains
- [ ] If it supersedes another, set the old one's Status **and** say what changed
- [ ] If it changes an inter-service shape, run `contract-check`
- [ ] If it records a newly discovered environment trap, add it to `mpria-next-own-memory.md` §7

Keep it short. The existing ADRs are 40–70 lines. Length is not evidence of rigour; a fairly stated
rejected alternative is.
