# Docker Deployment Approaches: Analysis

## Overview

This document compares two common approaches for deploying Java applications with Docker containers, and explores what enterprise companies actually use in production.

---

## The Two Approaches Compared

### Approach 1: Dockerfile + deploy.sh Script

```
Dockerfile (builds Java app + Payara)
deploy.sh (orchestrates containers manually)
```

**What this approach does:**
- Dockerfile handles the build and image creation
- Shell script manages `docker run` commands, network creation, volume mounts, container linking

**Pros:**
- Full control over deployment sequence
- Can add custom logic (health checks, rollback, environment detection)
- No additional tooling dependency
- Useful for very specific deployment requirements

**Cons:**
- Reinventing orchestration logic that already exists
- Error-prone (manual network/volume management)
- Harder to maintain and version
- No declarative state management
- Difficult to reproduce across environments

---

### Approach 2: Dockerfile + docker-compose.yml

```
Dockerfile (builds Java app + Payara)
docker-compose.yml (declarative orchestration)
```

**What this approach does:**
- Dockerfile still builds your application image
- Compose declaratively defines services, networks, volumes, dependencies

**Pros:**
- Declarative, version-controlled infrastructure
- Built-in dependency management (`depends_on`, healthchecks)
- Single command: `docker-compose up --build`
- Environment-specific overrides (`docker-compose.override.yml`)
- Easy to share and reproduce
- Industry standard for local development

**Cons:**
- Limited to single-host (without Swarm)
- Not suitable for production-scale orchestration
- Limited rollback capabilities

---

## What Big Companies Actually Do

**Neither approach is used in production at scale.** Here's the reality of enterprise deployments:

### Google, Meta, Netflix, etc. use:

| Layer | Tool |
|-------|------|
| **Build** | Bazel, Gradle, Maven (CI pipeline) |
| **Image Creation** | Jib (Google), Kaniko, BuildKit |
| **Registry** | Artifact Registry, ECR, Harbor |
| **Orchestration** | **Kubernetes** (GKE, EKS, self-managed) |
| **Deployment** | ArgoCD, Spinnaker, Flux (GitOps) |

### The Modern Enterprise Pattern

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   CI/CD     │────▶│  Container  │────▶│ Kubernetes  │
│  (Jenkins,  │     │  Registry   │     │   Cluster   │
│  GitHub     │     │             │     │             │
│  Actions)   │     │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘
      │                                        │
      ▼                                        ▼
  Build JAR/WAR                         Helm Charts or
  Build Image (Jib)                     Kustomize manifests
```

### Why Kubernetes?

- **Horizontal scaling**: Auto-scale based on load
- **Self-healing**: Automatic container restart on failure
- **Rolling updates**: Zero-downtime deployments
- **Service discovery**: Built-in DNS and load balancing
- **Secrets management**: Secure configuration handling
- **Multi-cloud**: Portable across cloud providers

---

## Recommended Project Structure

```
project/
├── src/                        # Java source code
├── pom.xml                     # Maven build file
├── Dockerfile                  # Multi-stage build
├── docker-compose.yml          # Dev/local orchestration
├── docker-compose.prod.yml     # Production overrides
├── docker-compose.test.yml     # Testing configuration
├── .env.example                # Environment template
└── k8s/                        # Kubernetes manifests (production)
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    └── ingress.yaml
```

---

## Example Configurations

### Dockerfile (Multi-stage Build)

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM payara/server-full:6.2024.6-jdk21
COPY --from=builder /app/target/*.war ${DEPLOY_DIR}/
EXPOSE 8080 4848
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: payara-app
    depends_on:
      db:
        condition: service_healthy
    environment:
      - DB_HOST=db
      - DB_PORT=5432
      - DB_NAME=myapp
      - DB_USER=${DB_USER}
      - DB_PASSWORD=${DB_PASSWORD}
    ports:
      - "8080:8080"
      - "4848:4848"
    networks:
      - app-network
    restart: unless-stopped

  db:
    image: postgres:16-alpine
    container_name: postgres-db
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d myapp"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - app-network
    restart: unless-stopped

volumes:
  pgdata:
    driver: local

networks:
  app-network:
    driver: bridge
```

### docker-compose.prod.yml (Production Overrides)

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        - BUILD_ENV=production
    environment:
      - JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC
      - DB_HOST=${PROD_DB_HOST}
    ports:
      - "8080:8080"
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 3G
        reservations:
          cpus: '1'
          memory: 2G
    logging:
      driver: json-file
      options:
        max-size: "100m"
        max-file: "5"

  db:
    environment:
      POSTGRES_USER: ${PROD_DB_USER}
      POSTGRES_PASSWORD: ${PROD_DB_PASSWORD}
    volumes:
      - /data/postgres:/var/lib/postgresql/data
```

### .env.example

```bash
# Database Configuration
DB_USER=appuser
DB_PASSWORD=changeme
DB_NAME=myapp

# Production Database (for docker-compose.prod.yml)
PROD_DB_HOST=prod-db.example.com
PROD_DB_USER=prod_appuser
PROD_DB_PASSWORD=secure_password

# Application Settings
JAVA_OPTS=-Xms256m -Xmx1g
```

---

## Usage Commands

### Development

```bash
# Build and start all services
docker-compose up --build

# Start in detached mode
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Production

```bash
# Use production overrides
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Scale application (if using Swarm)
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale app=3
```

---

## Decision Matrix

| Scenario | Recommended Approach |
|----------|---------------------|
| Local development | **docker-compose** |
| CI/CD integration tests | **docker-compose** |
| Small team / Simple prod | docker-compose + Swarm |
| Production at scale | **Kubernetes + Helm/ArgoCD** |
| Multi-region deployment | Kubernetes + Service Mesh |
| Legacy migration | docker-compose (transitional) |

---

## Migration Path to Production

1. **Phase 1**: docker-compose for local development
2. **Phase 2**: docker-compose in CI/CD for integration testing
3. **Phase 3**: Docker Swarm for simple production (optional)
4. **Phase 4**: Kubernetes with Helm charts for enterprise production
5. **Phase 5**: GitOps with ArgoCD/Flux for continuous deployment

---

## Conclusion

The **deploy.sh approach** is essentially a hand-rolled, less maintainable version of what docker-compose provides out of the box. Unless you have very specific scripting needs that compose cannot handle, **docker-compose is the recommended approach** for development and simple deployments.

For production environments at scale, the industry standard is **Kubernetes** with GitOps practices, which provides the reliability, scalability, and observability that enterprise applications require.
