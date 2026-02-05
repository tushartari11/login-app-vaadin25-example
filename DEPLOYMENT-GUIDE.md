# Authentication System Deployment Guide
## Vaadin 25 / Jakarta EE 10 / PostgreSQL / Payara 7

This guide provides step-by-step instructions for deploying and testing the authentication system using Docker Compose.

---

## 📋 Pre-Deployment Checklist

### Prerequisites Verification

- [ ] **Docker installed and running**
  - Run: `docker --version`
  - Expected: Docker version 20.10+ or higher
  - Run: `docker-compose --version`
  - Expected: Docker Compose version 2.0+ or higher

- [ ] **Optional: Java 21 and Maven** (only needed if building locally without Docker)
  - Run: `java -version`
  - Expected: `openjdk version "21"` or similar
  - Run: `mvn -version`
  - Expected: Apache Maven 3.8+

---

## 🚀 Quick Start (Docker Compose)

### Option 1: Development Deployment

The simplest way to get started is using Docker Compose. This will:
- Build the application from source
- Start PostgreSQL database
- Start Payara server
- Deploy the application
- Configure JDBC resources automatically

```bash
# Navigate to project directory
cd /path/to/vaadin-app

# Create environment configuration (optional, uses defaults if not created)
cp .env.example .env

# Start all services
docker-compose up --build

# Or start in detached mode (background)
docker-compose up --build -d

# View logs
docker-compose logs -f app
```

**Expected output:**
- PostgreSQL container starts first
- Payara container waits for PostgreSQL to be healthy
- Application builds and deploys automatically
- Application accessible at: http://localhost:8080/vaadin-payara-app/

- [ ] Containers started successfully
- [ ] Application accessible in browser
- [ ] No errors in container logs

---

### Option 2: Production Deployment

For production deployment with optimized settings:

```bash
# Create production environment file
cp .env.example .env
nano .env  # Edit with production values

# Start with production overrides
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# View logs
docker-compose logs -f
```

- [ ] Production containers started
- [ ] Production database configured
- [ ] Resource limits applied
- [ ] Logging configured

---

## 🗄️ Environment Configuration

### Creating .env File

Create a `.env` file in the project root (copy from `.env.example`):

```bash
# Development Configuration
DB_USER=vaadinuser
DB_PASSWORD=vaadinpass
DB_NAME=vaadindb
DB_PORT=5432

# Production Configuration (for docker-compose.prod.yml)
PROD_DB_HOST=your-prod-db-host.com
PROD_DB_PORT=5432
PROD_DB_USER=prod_vaadinuser
PROD_DB_PASSWORD=your_secure_password
PROD_DB_NAME=vaadindb_prod

# Application Settings
JAVA_OPTS=-Xms256m -Xmx1g
```

**For development:** Use default values from `.env.example`

**For production:** Update with secure credentials

- [ ] `.env` file created
- [ ] Database credentials configured
- [ ] Production values set (if applicable)

---

## 📦 Docker Compose Commands

### Starting Services

```bash
# Build and start all services
docker-compose up --build

# Start in detached mode (background)
docker-compose up -d

# Start specific service
docker-compose up -d db
docker-compose up -d app
```

### Viewing Logs

```bash
# View all logs
docker-compose logs

# Follow logs in real-time
docker-compose logs -f

# View specific service logs
docker-compose logs -f app
docker-compose logs -f db
```

### Stopping Services

```bash
# Stop all services (keeps volumes)
docker-compose down

# Stop and remove volumes (deletes database data!)
docker-compose down -v

# Stop without removing containers
docker-compose stop
```

### Rebuilding Application

```bash
# Rebuild only the app (after code changes)
docker-compose up --build app

# Force rebuild without cache
docker-compose build --no-cache app
docker-compose up -d app
```

### Database Management

```bash
# Connect to PostgreSQL database
docker exec -it vaadin-postgres psql -U vaadinuser -d vaadindb

# Backup database
docker exec vaadin-postgres pg_dump -U vaadinuser vaadindb > backup.sql

# Restore database
cat backup.sql | docker exec -i vaadin-postgres psql -U vaadinuser -d vaadindb
```

### Accessing Containers

```bash
# Access Payara container shell
docker exec -it vaadin-payara bash

# Access PostgreSQL container shell
docker exec -it vaadin-postgres bash

# View Payara admin console
# Open browser to: http://localhost:4848
```

---

## 🔍 Verification Steps

### Task 1: Verify Containers Running

```bash
# List running containers
docker ps

# Check container health
docker-compose ps
```

Expected output:
```
NAME               IMAGE                  STATUS
vaadin-postgres    postgres:15-alpine     Up (healthy)
vaadin-payara      vaadin-app_app         Up
```

- [ ] Both containers running
- [ ] PostgreSQL shows "healthy" status
- [ ] Payara container started successfully

### Task 2: Verify Database Schema

Connect to database and check tables:

```bash
docker exec -it vaadin-postgres psql -U vaadinuser -d vaadindb
```

Run SQL commands:

```sql
-- List all tables
\dt

-- Describe app_users table
\d app_users

-- Check test data
SELECT username, email, role, active FROM app_users;

-- Exit
\q
```

Expected:
- Table `app_users` exists
- 4 test users: admin, manager, user, guest
- All users are active

- [ ] Database schema created automatically
- [ ] Test users loaded
- [ ] Password hashes in BCrypt format

### Task 3: Access Application

Open browser and navigate to:

**http://localhost:8080/vaadin-payara-app/**

Expected: Should redirect to `/login` page

- [ ] Application accessible
- [ ] Login page renders correctly
- [ ] No 404 or 500 errors

---

## 🧪 Testing Authentication System

### Test Credentials

The following test users are automatically created:

| Username | Password    | Role    |
|----------|-------------|---------|
| admin    | Admin@123   | ADMIN   |
| manager  | Manager@123 | MANAGER |
| user     | User@123    | USER    |
| guest    | Guest@123   | GUEST   |

### Test Case 1: Login as USER

1. Navigate to: http://localhost:8080/vaadin-payara-app/login
2. Username: `user`
3. Password: `User@123`
4. Click "Login"

- [ ] Success notification appears
- [ ] Redirects to main page
- [ ] Header shows "Logged in as: user"

### Test Case 2: Login as ADMIN

1. Logout
2. Username: `admin`
3. Password: `Admin@123`
4. Click "Login"

- [ ] Successful login
- [ ] Can access admin panel at `/admin`

### Test Case 3: Registration Flow

1. Navigate to: http://localhost:8080/vaadin-payara-app/register
2. Username: `newuser`
3. Email: `newuser@example.com`
4. Password: `NewUser@123`
5. Confirm Password: `NewUser@123`
6. Click "Create Account"

- [ ] Success notification
- [ ] Redirects to login page
- [ ] Can login with new credentials

### Test Case 4: Invalid Login

1. Navigate to login page
2. Username: `admin`
3. Password: `WrongPassword`
4. Click "Login"

- [ ] Error message appears
- [ ] Does not grant access
- [ ] Generic error (doesn't reveal if user exists)

### Test Case 5: Admin Panel Access Control

1. Login as `user` (not admin)
2. Navigate to: http://localhost:8080/vaadin-payara-app/admin

- [ ] Access denied (403 Forbidden)
- [ ] Cannot see admin panel

3. Logout and login as `admin`
4. Navigate to: http://localhost:8080/vaadin-payara-app/admin

- [ ] Admin panel accessible
- [ ] User grid displays all users
- [ ] Can lock/unlock user accounts

---

## 🛠️ Troubleshooting

### Issue: Containers won't start

```bash
# Check Docker daemon is running
docker info

# Check for port conflicts
lsof -i :5432  # PostgreSQL port
lsof -i :8080  # Application port
lsof -i :4848  # Payara admin port

# View container logs
docker-compose logs
```

Solution:
- [ ] Ensure Docker Desktop is running
- [ ] Check no other services using ports 5432, 8080, 4848
- [ ] Run `docker-compose down -v` and try again

### Issue: Database connection fails

```bash
# Check PostgreSQL health
docker exec vaadin-postgres pg_isready -U vaadinuser

# View database logs
docker-compose logs db

# Test connection from Payara container
docker exec -it vaadin-payara curl -v postgres:5432
```

Solution:
- [ ] Wait for PostgreSQL health check to pass
- [ ] Check environment variables in .env file
- [ ] Verify `depends_on` with `service_healthy` condition

### Issue: Application deployment fails

```bash
# View Payara server logs
docker-compose logs app

# Check if WAR file was created
docker exec vaadin-payara ls -la /opt/payara/deployments/

# Check Payara domain logs
docker exec vaadin-payara tail -100 /opt/payara/appserver/glassfish/domains/domain1/logs/server.log
```

Solution:
- [ ] Check for build errors in logs
- [ ] Verify PostgreSQL is healthy before Payara starts
- [ ] Check JDBC post-boot commands executed correctly

### Issue: "Permission denied" errors

```bash
# Fix ownership issues
docker-compose down
sudo chown -R $USER:$USER .
docker-compose up --build
```

### Issue: Need to reset everything

```bash
# Complete cleanup
docker-compose down -v              # Remove containers and volumes
docker system prune -a              # Remove unused images
docker-compose up --build           # Fresh start
```

---

## 🔒 Production Deployment Checklist

### Before Production

- [ ] **Remove test data loader**
  - Comment out or delete `DataLoader.java`
  - Or set conditional flag to disable in production

- [ ] **Secure environment variables**
  - Generate strong database password: `openssl rand -base64 32`
  - Update `.env` file with production values
  - Never commit `.env` to version control (add to `.gitignore`)

- [ ] **Change JPA DDL generation**
  - Edit `persistence.xml`
  - Set `eclipselink.ddl-generation` to `none`
  - Use Flyway or Liquibase for schema migrations

- [ ] **Configure resource limits**
  - Update `docker-compose.prod.yml` with appropriate CPU/memory limits
  - Test resource allocation under load

- [ ] **Enable HTTPS**
  - Configure reverse proxy (nginx, Traefik)
  - Obtain SSL certificate (Let's Encrypt)
  - Update Payara to use HTTPS

- [ ] **Set up monitoring**
  - Configure health checks
  - Set up log aggregation (ELK, CloudWatch)
  - Configure alerts for failures

- [ ] **Database backups**
  - Set up automated PostgreSQL backups
  - Test restore procedure
  - Configure backup retention policy

- [ ] **Production logging**
  - Set log level to INFO or WARNING
  - Configure log rotation
  - Ensure sensitive data not logged

---

## 📊 Architecture Overview

### Docker Compose Services

```
┌─────────────────────────────────────────┐
│         Docker Compose Network          │
│                                         │
│  ┌──────────────┐    ┌──────────────┐  │
│  │  PostgreSQL  │    │    Payara    │  │
│  │   (db)       │◄───┤    (app)     │  │
│  │              │    │              │  │
│  │  Port: 5432  │    │  Port: 8080  │  │
│  │              │    │  Port: 4848  │  │
│  └──────────────┘    └──────────────┘  │
│         │                    │          │
│    pgdata volume      payara-logs vol   │
└─────────────────────────────────────────┘
         │                    │
         └────────┬───────────┘
                  │
          Host Machine Ports
         5432, 8080, 4848, 8181
```

### Build Process

```
1. Maven Multi-stage Build
   ├── Stage 1: Build WAR file
   │   ├── Download dependencies
   │   ├── Compile Java sources
   │   ├── Build Vaadin frontend
   │   └── Package WAR
   │
   └── Stage 2: Runtime Image
       ├── Copy WAR from Stage 1
       ├── Configure Payara
       ├── Copy post-boot JDBC commands
       └── Deploy application

2. Startup Sequence
   ├── PostgreSQL starts
   ├── Health check waits for pg_isready
   ├── Payara starts after PostgreSQL healthy
   ├── Post-boot commands create JDBC pool
   └── Application deployed and ready
```

---

## 📞 Support and Resources

### Official Documentation

- **Docker Compose:** https://docs.docker.com/compose/
- **Payara Server:** https://docs.payara.fish/
- **Jakarta EE 10:** https://jakarta.ee/specifications/platform/10/
- **Vaadin 25:** https://vaadin.com/docs/latest/
- **PostgreSQL:** https://www.postgresql.org/docs/

### Project Documentation

- [Docker Deployment Approaches](docker-deployment-approaches.md)
- [Coding Guidelines](CODING-GUIDELINES.md)
- [Project Standards (CLAUDE.md)](CLAUDE.md)

### Common Commands Quick Reference

```bash
# === Docker Compose Commands ===

# Start services
docker-compose up -d

# Rebuild and start
docker-compose up --build

# Stop services
docker-compose down

# View logs
docker-compose logs -f app

# === Database Commands ===

# Connect to database
docker exec -it vaadin-postgres psql -U vaadinuser -d vaadindb

# Backup database
docker exec vaadin-postgres pg_dump -U vaadinuser vaadindb > backup.sql

# === Application Commands ===

# View Payara logs
docker exec vaadin-payara tail -f /opt/payara/appserver/glassfish/domains/domain1/logs/server.log

# Restart Payara (from inside container)
docker exec vaadin-payara asadmin restart-domain

# === Cleanup Commands ===

# Remove containers (keep volumes)
docker-compose down

# Remove containers and volumes (DELETES DATA!)
docker-compose down -v

# Clean everything
docker-compose down -v && docker system prune -a
```

---

## 🎯 Next Steps

After successful deployment:

1. **Test all authentication flows** (login, registration, logout)
2. **Test admin panel functionality** (lock/unlock users)
3. **Verify accessibility** (keyboard navigation, screen reader)
4. **Review security settings** (HTTPS, session timeout, CSRF)
5. **Set up monitoring** (health checks, log aggregation)
6. **Plan production migration** (see Production Checklist above)

---

**Deployment guide complete! Use this document as a step-by-step checklist to deploy and test your authentication system using Docker Compose.**
