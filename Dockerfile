# syntax=docker/dockerfile:1

# Build stage: multi-module reactor (domain, application, infrastructure, ui-vaadin)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY domain domain
COPY application application
COPY infrastructure infrastructure
COPY ui-vaadin ui-vaadin
ARG MAVEN_PROFILE=production
# The cache mount keeps ~/.m2 and Vaadin's Node download (~/.vaadin) between builds.
# (dependency:go-offline cannot resolve the not-yet-built sibling modules, so it is not used.)
RUN --mount=type=cache,target=/root/.m2 --mount=type=cache,target=/root/.vaadin \
    mvn clean package -P${MAVEN_PROFILE} -DskipTests -B

# Download PostgreSQL JDBC driver in builder stage (curl available here).
# Keep in step with postgresql.version in the parent pom.xml.
ARG POSTGRESQL_VERSION=42.7.8
RUN curl -fsSL -o /tmp/postgresql-${POSTGRESQL_VERSION}.jar \
    https://jdbc.postgresql.org/download/postgresql-${POSTGRESQL_VERSION}.jar

# Runtime stage: Payara 7 = Jakarta EE 11 (required by Vaadin 25 / Servlet 6.1)
FROM payara/server-full:7.2025.1

# Copy PostgreSQL JDBC driver from builder into Payara's domain lib
ARG POSTGRESQL_VERSION=42.7.8
COPY --from=builder /tmp/postgresql-${POSTGRESQL_VERSION}.jar \
    ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/lib/postgresql-${POSTGRESQL_VERSION}.jar

# Enable remote admin access via pre-boot commands
COPY --chown=payara:payara payara-pre-boot.asadmin ${PAYARA_DIR}/
ENV PREBOOT_COMMANDS=${PAYARA_DIR}/payara-pre-boot.asadmin

# Copy post-boot commands for JDBC setup
COPY --chown=payara:payara payara-post-boot.asadmin ${PAYARA_DIR}/

# Set the post-boot command file
ENV POSTBOOT_COMMANDS=${PAYARA_DIR}/payara-post-boot.asadmin

# Deploy the application WAR (built by the ui-vaadin module)
COPY --from=builder /app/ui-vaadin/target/vaadin-payara-app.war ${DEPLOY_DIR}/

EXPOSE 8080 8181 4848
