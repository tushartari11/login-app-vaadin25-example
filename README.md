# Vaadin 25 Application for Payara 6

A scaffolded Vaadin 25 application designed to run on GlassFish Payara 6 with Java 21.

## Technology Stack

- **Java**: 21
- **Vaadin**: 25.0.0
- **Jakarta EE**: 10.0.0
- **Application Server**: GlassFish Payara 6
- **Build Tool**: Maven
- **Container**: Docker (Payara Server Full 6.2025.11, JDK 21)
- **Database**: PostgreSQL (JDBC driver 42.7.5)

## Project Structure

```
vaadin-payara-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/vaadin/
│   │   │       ├── Application.java          # Main application config
│   │   │       ├── views/
│   │   │       │   └── MainView.java         # Main view with routing
│   │   │       ├── services/
│   │   │       │   └── GreetingService.java  # Example CDI service
│   │   │       └── entities/
│   │   │           └── User.java             # Example entity
│   │   ├── resources/
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           ├── web.xml                   # Jakarta EE web config
│   │           └── beans.xml                 # CDI configuration
├── Dockerfile                                    # Multi-stage Docker build
├── .dockerignore                                 # Docker build context exclusions
└── pom.xml                                       # Maven configuration
```

## Prerequisites

1. **Java 21** - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
2. **Maven 3.8+** - [Download Maven](https://maven.apache.org/download.cgi)
3. **Payara 6** - [Download Payara Server](https://www.payara.fish/downloads/payara-platform-community-edition/)

## Setup Instructions

### 1. Verify Java 21 Installation

```bash
java -version
# Should show version 21.x.x
```

### 2. Build the Application

```bash
cd vaadin-payara-app
mvn clean package
```

This will:
- Compile the Java sources
- Prepare Vaadin frontend resources
- Build the WAR file at `target/vaadin-payara-app.war`

### 3. Deploy to Payara 6

#### Option A: Using Payara Admin Console

1. Start Payara Server:
   ```bash
   ./payara6/bin/asadmin start-domain
   ```

2. Access Admin Console at http://localhost:4848

3. Navigate to Applications → Deploy

4. Upload `target/vaadin-payara-app.war`

5. Access your application at http://localhost:8080/vaadin-payara-app

#### Option B: Using Command Line

```bash
# Start Payara
./payara6/bin/asadmin start-domain

# Deploy the application
./payara6/bin/asadmin deploy target/vaadin-payara-app.war

# Access at http://localhost:8080/vaadin-payara-app
```

#### Option C: Autodeploy

1. Start Payara Server
2. Copy the WAR file to Payara's autodeploy directory:
   ```bash
   cp target/vaadin-payara-app.war /path/to/payara6/glassfish/domains/domain1/autodeploy/
   ```

### Option D: Using Docker

See [Docker Deployment](#docker-deployment) below for building and running with Docker.

## Docker Deployment

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) installed and running

### Docker Image Details

The Dockerfile uses a multi-stage build:

| Stage | Base Image | Purpose |
|-------|-----------|---------|
| Build | `maven:3.9-eclipse-temurin-21` | Compiles source and builds production WAR |
| Runtime | `payara/server-full:6.2025.11-jdk21` | Payara 6 with JDK 21 |

The runtime image includes:
- **Payara Server Full 6** (Jakarta EE 10 certified)
- **JDK 21**
- **PostgreSQL JDBC driver 42.7.5** (installed in Payara domain `lib/`)
- Production-optimized Vaadin frontend bundle

### Build the Docker Image

```bash
docker build -t vaadin-payara-app .
```

This runs the production Maven build (`-Pproduction`) inside the container and produces an optimized image.

### Run the Container

```bash
docker run -d --name vaadin-app -p 8080:8080 payara-vaadin-app
```

Access the application at http://localhost:8080/vaadin-payara-app

### Exposed Ports

| Port | Service |
|------|---------|
| 8080 | HTTP (application) |
| 8181 | HTTPS |
| 4848 | Payara Admin Console |

To expose the admin console as well:

```bash
docker run -d --name vaadin-app -p 8080:8080 -p 4848:4848 vaadin-payara-app
```

### Running with PostgreSQL

Use Docker Compose or link containers manually:

```bash
# Create a network
docker network create vaadin-net

# Start PostgreSQL
docker run -d --name postgres \
  --network vaadin-net \
  -e POSTGRES_DB=vaadindb \
  -e POSTGRES_USER=vaadin \
  -e POSTGRES_PASSWORD=vaadin \
  -p 5432:5432 \
  postgres:17

# Start the application
docker run -d --name vaadin-app \
  --network vaadin-net \
  -p 8080:8080 \
  vaadin-payara-app
```

After both containers are running, configure the JDBC connection pool in Payara via `asadmin`:

```bash
docker exec vaadin-app ${PAYARA_DIR}/bin/asadmin create-jdbc-connection-pool \
  --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
  --restype javax.sql.DataSource \
  --property serverName=postgres:portNumber=5432:databaseName=vaadindb:user=vaadin:password=vaadin \
  PostgresPool

docker exec vaadin-app ${PAYARA_DIR}/bin/asadmin create-jdbc-resource \
  --connectionpoolid PostgresPool \
  jdbc/vaadindb
```

### Customizing the PostgreSQL Driver Version

The driver version can be overridden at build time:

```bash
docker build --build-arg POSTGRESQL_VERSION=42.7.9 -t vaadin-payara-app .
```

### Stopping and Removing Containers

```bash
docker stop vaadin-app postgres
docker rm vaadin-app postgres
docker network rm vaadin-net
```

## Development Mode

For development with hot reload:

```bash
mvn clean package
# Then deploy the WAR to Payara as described above
```

Note: Vaadin's hot reload works best with IDE integration. For full development mode features, consider using the embedded server profile during development and Payara for production.

## Production Build

To create an optimized production build:

```bash
mvn clean package -Pproduction
```

This will create an optimized bundle with minified resources.

## Key Features

### CDI Integration

The application uses Jakarta CDI for dependency injection:

```java
@Route("")
public class MainView extends VerticalLayout {
    @Inject
    private GreetingService greetingService;
    // ...
}
```

### Routing

Views are automatically registered using `@Route` annotation:

```java
@Route("")  // Maps to root URL
@AnonymousAllowed
public class MainView extends VerticalLayout {
    // ...
}
```

### Jakarta EE 10

Full access to Jakarta EE 10 APIs:
- CDI (Contexts and Dependency Injection)
- JPA (Java Persistence API)
- JAX-RS (RESTful Web Services)
- And more...

## Common Customizations

### Adding JPA Support

1. Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.eclipse.persistence</groupId>
    <artifactId>eclipselink</artifactId>
    <version>4.0.2</version>
    <scope>provided</scope>
</dependency>
```

2. Create `src/main/resources/META-INF/persistence.xml`

### Adding REST Endpoints

1. Create a JAX-RS Application class:
```java
@ApplicationPath("/api")
public class RestApplication extends Application {
}
```

2. Create resource classes with `@Path`, `@GET`, etc.

### Customizing Vaadin Theme

1. Create `frontend/themes/my-theme/` directory
2. Add `styles.css` and `theme.json`
3. Customize colors, fonts, and components

## Troubleshooting

### Port Conflicts

If port 8080 is in use, modify Payara's port:
```bash
./payara6/bin/asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8888
```

### ClassLoader Issues

If you encounter ClassLoader issues, ensure Payara 6 is properly configured for Jakarta EE 10.

### Build Failures

Clear Maven cache and rebuild:
```bash
mvn clean install -U
```

## Additional Resources

- [Vaadin Documentation](https://vaadin.com/docs/latest)
- [Payara Documentation](https://docs.payara.fish/)
- [Jakarta EE 10 Specification](https://jakarta.ee/specifications/platform/10/)
- [CDI Documentation](https://jakarta.ee/specifications/cdi/)

## License

This is a scaffolding project for development purposes.
# login-app-vaadin25-example
