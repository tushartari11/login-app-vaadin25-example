# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
ARG MAVEN_PROFILE=production
RUN mvn clean package -P${MAVEN_PROFILE} -DskipTests -B

# Download PostgreSQL JDBC driver in builder stage (curl available here)
ARG POSTGRESQL_VERSION=42.7.5
RUN curl -fsSL -o /tmp/postgresql-${POSTGRESQL_VERSION}.jar \
    https://jdbc.postgresql.org/download/postgresql-${POSTGRESQL_VERSION}.jar

# Runtime stage
FROM payara/server-full:7.2025.1

# Copy PostgreSQL JDBC driver from builder into Payara's domain lib
ARG POSTGRESQL_VERSION=42.7.5
COPY --from=builder /tmp/postgresql-${POSTGRESQL_VERSION}.jar \
    ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/lib/postgresql-${POSTGRESQL_VERSION}.jar

# Enable remote admin access via pre-boot commands
COPY --chown=payara:payara payara-pre-boot.asadmin ${PAYARA_DIR}/
ENV PREBOOT_COMMANDS=${PAYARA_DIR}/payara-pre-boot.asadmin

# Copy post-boot commands for JDBC setup
COPY --chown=payara:payara payara-post-boot.asadmin ${PAYARA_DIR}/

# Set the post-boot command file
ENV POSTBOOT_COMMANDS=${PAYARA_DIR}/payara-post-boot.asadmin

# Deploy the application WAR
COPY --from=builder /app/target/*.war ${DEPLOY_DIR}/

EXPOSE 8080 8181 4848
