# Vaadin 25 Dependency Upgrade Guide

Nov 14, 2026 · @TusharT

## Overview

Move off `vaadin.version` 25.0.0 to the latest 25.x maintenance release (25.3.4 per the Vaadin
upgrade docs at time of writing), and change `jakarta.version` from 10.0.0 to Jakarta EE 11, or
remove it entirely. Vaadin only maintains the latest minor, so 25.0.0 is missing almost a year of
fixes. Vaadin 25.4 is targeted for Q4 2026.

| Requirement             | Vaadin 24 | Vaadin 25 |
|-------------------------|-----------|-----------|
| Java                    | 17+       | 21+       |
| Spring Boot / Framework | 3.x / 6   | 4.1+ / 7  |
| Jakarta EE              | 10        | 11        |
| Servlet                 | 6.0       | 6.1       |
| Jackson                 | 2         | 3         |
| Node.js                 | 20+       | 24+       |
| Quarkus                 | —         | 3.27+     |

Current setting to fix:

```xml

<vaadin.version>25.0.0</vaadin.version>   <!-- outdated minor -->
<jakarta.version>10.0.0</jakarta.version>  <!-- wrong: Vaadin 25 targets Jakarta EE 11 -->
```

## Step 1 — Manage Vaadin through the BOM

Import `vaadin-bom` once and control everything with a single `vaadin.version` property. Never set a
`<version>` on an individual `com.vaadin` artifact, because the BOM keeps Flow, the web components
and Hilla in step with each other.

```xml

<properties>
  <java.version>21</java.version>
  <vaadin.version>25.3.4</vaadin.version> <!-- check github.com/vaadin/platform/releases -->
</properties>

<dependencyManagement>
<dependencies>
  <dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-bom</artifactId>
    <version>${vaadin.version}</version>
    <type>pom</type>
    <scope>import</scope>
  </dependency>
</dependencies>
</dependencyManagement>

<dependencies>
<dependency>
  <groupId>com.vaadin</groupId>
  <artifactId>vaadin</artifactId> <!-- or vaadin-core; no <version> -->
</dependency>
</dependencies>
```

## Step 2 — Automate bumps instead of unpinning

Keep an explicit version and bump it often. `LATEST`, `RELEASE` and ranges like `[25,26)` make
builds non-reproducible and can pull in a broken release overnight. Vaadin's guidance is to set the
latest release explicitly.

Add `versions-maven-plugin` with a rule that skips pre-releases:

```xml

<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>versions-maven-plugin</artifactId>
  <version>2.18.0</version>
  <configuration>
    <ruleSet>
      <ignoreVersions>
        <ignoreVersion>
          <type>regex</type>
          <version>(?i).*(alpha|beta|rc|-M\d+|snapshot).*</version>
        </ignoreVersion>
      </ignoreVersions>
    </ruleSet>
  </configuration>
</plugin>
```

```bash
mvn versions:display-property-updates     # see what's available
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
mvn versions:update-properties -DallowMajorUpdates=false   # stays on 25.x
mvn versions:update-parent -DallowMajorUpdates=false       # Spring Boot parent, if used
```

In CI, add Renovate or Dependabot:

- Patch updates (25.3.x): auto-merge when the build is green.
- Minor updates (25.x): open a PR and read the release notes before merging.
- Major updates (26): manual, following the Vaadin upgrade guide.

## Step 3 — Let the platform own the Jakarta version

Delete the hand-set `jakarta.version` and let your runtime platform decide it.

**Spring Boot apps.** Vaadin 25 requires Spring Boot 4.1 or newer (Spring Framework 7). The Boot
parent manages the Jakarta APIs, so you need no Jakarta property:

```xml

<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.1.x</version>
  <relativePath/>
</parent>
```

**Plain Jakarta EE servers (Payara, WildFly and others).** Use the EE 11 API as `provided`, and run
on a server certified for Jakarta EE 11:

```xml

<dependency>
  <groupId>jakarta.platform</groupId>
  <artifactId>jakarta.jakartaee-api</artifactId> <!-- or jakarta.jakartaee-web-api -->
  <version>11.0.0</version>
  <scope>provided</scope>
</dependency>
```

Check "Supported technologies" in the release notes for your exact server. For example, the latest
WildFly versions still use Jackson 2, so they need a `jboss-deployment-structure.xml` under
`WEB-INF` to run Vaadin 25.

## Steps 4–5 — Align the plugin and clean the frontend

Point `vaadin-maven-plugin` at the same property, so the plugin and the runtime never drift apart:

```xml

<plugin>
  <groupId>com.vaadin</groupId>
  <artifactId>vaadin-maven-plugin</artifactId>
  <version>${vaadin.version}</version>
</plugin>
```

After every Vaadin bump, clear stale `node_modules`, lock files and generated sources, then rebuild.
Node.js 24+ is required.

```bash
mvn vaadin:clean-frontend
mvn clean verify
mvn clean package -Pproduction
```

## Best-practice checklist

- [ ] Clear all deprecation warnings before moving to the next minor; deprecated APIs are removed at
  the next major.
- [ ] Read the release notes for every minor (25.1, 25.2, 25.3, …) for feature flags and behaviour
  changes.
- [ ] Check for Jackson 3 breakage in custom serialization; search for leftover
  `com.fasterxml.jackson.databind` imports.
- [ ] Spring Security: replace the removed `VaadinWebSecurity` with `VaadinSecurityConfigurer`.
- [ ] Confirm every Directory add-on has a Vaadin 25 compatible release, because the BOM doesn't
  cover add-ons.
- [ ] For a Spring Boot 3.5 → 4 jump, run the OpenRewrite recipe
  `org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0` before fixing things by hand.
- [ ] Verify the Node.js version (24+) on dev machines and CI runners.

## Recommended workflow

1. Renovate or Dependabot opens a PR that bumps `vaadin.version` (and the Spring Boot parent, if you
   use it).
2. CI runs `mvn vaadin:clean-frontend`, then `mvn clean verify` with the UI/browser tests.
3. Patch PRs auto-merge; minor PRs get a release-notes review and then merge.
4. Run `mvn versions:display-dependency-updates` monthly to catch dependencies the bots don't
   manage.

## Sources

- [Vaadin releases & roadmap](https://vaadin.com/roadmap)
- [Upgrading Vaadin (docs)](https://vaadin.com/docs/upgrading)
- [Vaadin version comparison](https://vaadin.com/docs/next/upgrading/version-comparison)
- [Vaadin 25.0.0 release notes](https://newreleases.io/project/github/vaadin/platform/release/25.0.0)
- [Vaadin 25.3.0 release notes](https://newreleases.io/project/github/vaadin/platform/release/25.3.0)
- [Vaadin 24 free maintenance ends: choose your path](https://blog.vaadin.com/vaadin-24-free-maintenance-ends-june-16-choose-your-path-forward)
- [Spring Boot 4 migration playbook (Vaadin blog)](https://blog.vaadin.com/spring-boot-4-migration-playbook-for-teams-upgrading-from-3.5)
- [Vaadin platform releases on GitHub](https://github.com/vaadin/platform/releases)
