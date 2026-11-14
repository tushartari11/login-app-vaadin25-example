# Vaadin 25 / Payara 7 Application (DDD / hexagonal layout)

Login, self-registration and user administration built with **Vaadin 25.3**, **Java 21**,
**Jakarta EE 11** and **PostgreSQL**, deployed as a WAR on **Payara 7**.

```
vaadin-app/
├── pom.xml            parent: versions, BOMs, enforcer, versions-plugin rules
├── domain/            pure Java: User aggregate, value objects, password policy, repository PORTS
├── application/       use cases + DTOs (@Inject / @Transactional only)
├── infrastructure/    JPA entity + repository adapter, bcrypt, Jakarta Security identity store, dev seed data
└── ui-vaadin/         Vaadin views, theme, CSS -> ui-vaadin/target/vaadin-payara-app.war
```

```
ui-vaadin ──compile──▶ application ──compile──▶ domain
    │                                              ▲
    └──runtime──▶ infrastructure ──compile─────────┘
```

| Module         | May depend on                                   | Enforced by                                               |
|----------------|-------------------------------------------------|-----------------------------------------------------------|
| domain         | nothing                                         | no deps in POM + `DomainPurityTest` (ArchUnit)            |
| application    | domain                                          | POM                                                       |
| infrastructure | domain, application, Jakarta EE API             | POM                                                       |
| ui-vaadin      | application (compile); domain + infra (runtime) | domain excluded from compile classpath + `UiBoundaryTest` |

A Vaadin view that imports a domain class, a JPA entity or a repository **does not compile**.
Views get data only as DTOs (`UserView`) from use cases (`RegisterUserUseCase`,
`AuthenticateUserUseCase`,
`UserAdministrationUseCase`).

### Where things live

| Concern                                         | Class                                                                       |
|-------------------------------------------------|-----------------------------------------------------------------------------|
| Username / e-mail / password rules              | `domain.user.Username`, `Email`, `PasswordPolicy`                           |
| Lock / unlock, last-login, can-log-in           | `domain.user.User`                                                          |
| `app_users` table mapping                       | `infrastructure.persistence.UserEntity` + `UserMapper`                      |
| Credential check for Jakarta Security           | `infrastructure.security.DatabaseIdentityStore` → `AuthenticateUserUseCase` |
| Programmatic login / logout from views          | `ui.security.SecurityService`, `VaadinAuthenticationMechanism`              |
| Development users (admin, manager, user, guest) | `infrastructure.bootstrap.DevUserSeeder`                                    |

## Prerequisites

- Java 21+ and Maven 3.9+ (checked by the enforcer plugin)
- Node.js 24+ is required by Vaadin 25; if an older one is installed, Vaadin downloads its own into
  `~/.vaadin`
- Payara Server 7 (Jakarta EE 11). Payara 6 is Jakarta EE 10 and cannot run Vaadin 25 (Servlet 6.1).
- PostgreSQL

## Build

```bash
mvn clean verify                           # dev build (Vaadin dev mode) + unit and architecture tests
mvn clean package -Pproduction             # production WAR: ui-vaadin/target/vaadin-payara-app.war
mvn -pl ui-vaadin vaadin:clean-frontend    # after every Vaadin upgrade
```

The `development` profile is active by default and adds `vaadin-dev`; `-Pproduction` switches it off
and precompiles the frontend bundle instead.

## Run with Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

The app is at http://localhost:8080/vaadin-payara-app/ (Payara admin console on port 4848).
The image is built with `-Pproduction` by default. See [DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md).

## Run on a local Payara 7

1. Put the PostgreSQL JDBC driver into `<payara>/glassfish/domains/domain1/lib` (the WAR does not
   ship it).
2. Create the datasource used by `infrastructure/src/main/resources/META-INF/persistence.xml`:

   ```bash
   asadmin create-jdbc-connection-pool --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
     --restype javax.sql.DataSource \
     --property user=vaadinuser:password=vaadinpass:serverName=localhost:portNumber=5432:databaseName=vaadindb \
     VaadinAuthPool
   asadmin create-jdbc-resource --connectionpoolid VaadinAuthPool jdbc/VaadinAuthDS
   ```

3. Deploy: `asadmin deploy ui-vaadin/target/vaadin-payara-app.war`

On an empty database `DevUserSeeder` creates `admin / Admin@123`, `manager / Manager@123`,
`user / User@123` and `guest / Guest@123`. **Remove it before going to production.**

## Replacing the UI later

1. Add a module, e.g. `ui-rest` (JAX-RS) or `ui-spring`.
2. Copy the three dependencies from `ui-vaadin/pom.xml` (application compile + domain exclusion,
   domain/infra runtime).
3. Delete `ui-vaadin`. Nothing else changes.

Domain and application stay untouched: `jakarta.inject` and `jakarta.transaction` are honoured by
Spring and Quarkus too.

## Keeping dependencies current

Versions are explicit and bumped deliberately (see `docs/Vaadin 25 Dependency Upgrade Guide.md`):

```bash
mvn versions:display-property-updates
mvn versions:display-plugin-updates
mvn versions:update-properties -DallowMajorUpdates=false   # stays on 25.x
```

Pre-releases (alpha/beta/RC/milestones) are ignored by the rule in the parent POM. The enforcer also
bans
`javax.servlet`, `javax.persistence` and `org.mindrot:jbcrypt`, and requires dependency convergence.
