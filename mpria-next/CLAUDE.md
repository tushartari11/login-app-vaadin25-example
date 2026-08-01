# CLAUDE.md

Guidance for Claude Code working in this repository.

## Read this first

**[`mpria-next-own-memory.md`](mpria-next-own-memory.md) is the project handover document.** It
explains what this project is, the decisions the user already made, exact build commands, current
verified state, the bugs found so far, and every environment trap that cost time. Read it before
doing anything else — it is imported below so it loads with this file.

## The three rules most easily broken here

1. **This is not `framework-vaadin8`.** It is a greenfield successor in a sibling directory. Java 8,
   Maven 3.9.11 pinning and the applet-plugin `JAVA_HOME` trap are v8 constraints and do **not**
   apply. This project is Java 21 / Spring Boot 4.1.
2. **Off-VPN, Maven needs `-s .mvn/settings-central.xml`** or it times out against the corporate
   Nexus. On VPN, drop the flag.
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
   mvn -B -s .mvn/settings-central.xml clean verify
   ```
3. **`docs/CONTRACTS.md` is binding.** Services are built against it independently, so changing a
   field name there breaks something already shipped. Propose changes; do not make them silently.

## Non-negotiables

- **Tests first.** No production class before a failing test names it.
- **The domain imports nothing** — no Spring, JPA, Jackson or HTTP under `domain/`. `ArchitectureTest`
  fails the build. This is the boundary MpRIA v8 lost gradually.
- **No domain vocabulary in framework code.** The framework knows `RecordType`, `Field`, `Relation`
  — never `Artist`, `Painting` or `Museum`. Customer field names belong in fixtures and
  configuration only.
- **Every service needs a context-loads `@SpringBootTest`.** Slices pass while an application is
  unbootable; that has already happened once here.
- **Hibernate, not EclipseLink** — a Maven Enforcer rule bans `org.eclipse.persistence:*`.
- British-English spelling in prose.

@mpria-next-own-memory.md
