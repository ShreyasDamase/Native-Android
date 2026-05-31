# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 11 — Deployment: Getting HireStory Live on Railway

### _From localhost to the internet — environment variables, migrations, health checks, and production readiness_

---

## 11.1 The Problem This Chapter Solves

Your backend works perfectly on your machine. Now you need it to work on a server somewhere on the internet, running 24/7, accessible to your Android app and Next.js web app.

Deployment is where many developers hit a wall. The code is fine. The infrastructure is confusing. This chapter removes that confusion.

By the end of this chapter:

- Your Spring Boot app is running on Railway
- PostgreSQL, Redis, and RabbitMQ are provisioned and connected
- All environment variables are set correctly
- Flyway runs migrations automatically on every deploy
- Your app restarts automatically if it crashes
- You can check health and logs from anywhere
- Your Android app is talking to the production URL

---

## 11.2 What Railway Is

Railway is a cloud platform that runs your applications without requiring you to manage servers, Docker, or Kubernetes.

You connect your GitHub repo. Railway detects it is a Spring Boot app. It builds it and runs it. When you push code, Railway redeploys automatically.

For HireStory you need four Railway services:

- **Spring Boot app** — your backend
- **PostgreSQL** — your database
- **Redis** — your cache and counters
- **RabbitMQ** — your message queue

Railway gives you all four. They are connected on a private internal network. Your app talks to them using environment variables Railway provides automatically.

---

## 11.3 Preparing Your App for Production

Before deploying, several things in your codebase need to be production-ready. Do these locally first.

### 11.3.1 Production application.yml

Create `application-prod.yml` — this profile activates on Railway:

```yaml
# src/main/resources/application-prod.yml

spring:
  jpa:
    hibernate:
      ddl-auto: validate      # Never create or drop in production
    properties:
      hibernate:
        show_sql: false       # Never log SQL in production
        format_sql: false
  flyway:
    enabled: true             # Always run migrations on startup

logging:
  level:
    root: WARN
    com.example.hirestory: INFO
    org.springframework.security: WARN   # Remove verbose security logs
    org.hibernate: WARN

# Production server settings
server:
  port: ${PORT:8080}          # Railway sets PORT env var automatically
  shutdown: graceful          # Wait for in-flight requests to finish before shutting down
  tomcat:
    connection-timeout: 5s
    max-connections: 200

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s   # Give requests 30s to complete on shutdown
```

### 11.3.2 The Dockerfile

Railway can build from a Dockerfile or detect your framework automatically. A Dockerfile gives you more control and faster builds:

```dockerfile
# Dockerfile — place in root of your project

# Stage 1: Build
# Use the official Gradle image with JDK 21
FROM gradle:8.11-jdk21 AS builder

WORKDIR /app

# Copy build files first — Docker caches these layers
# If only your source code changes, Gradle dependencies are not re-downloaded
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/

# Download dependencies (cached layer — only re-runs if build files change)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the JAR — skip tests here (tests run in CI, not during image build)
RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
# Smaller image — only JRE, not full JDK
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user — security best practice
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy the JAR from the build stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port Spring Boot listens on
EXPOSE 8080

# JVM tuning for containers
# -XX:+UseContainerSupport — JVM respects container memory limits
# -XX:MaxRAMPercentage=75.0 — use 75% of container RAM for heap
# -Djava.security.egd — faster startup on Linux containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

### 11.3.3 .dockerignore

Prevent large unnecessary files from being sent to Docker:

```
# .dockerignore
.git
.gradle
build/
*.md
.idea/
*.iml
src/test/
```

### 11.3.4 Verify Your Build Locally First

Before deploying, verify the Docker build works on your machine:

```bash
# Build the image
docker build -t hirestory-api .

# Run it locally with test environment variables
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/hirestory_dev \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=yourpassword \
  hirestory-api

# Check it starts
curl http://localhost:8080/api/actuator/health
# Expected: {"status":"UP"}
```

If it starts locally with Docker, it will start on Railway.

---

## 11.4 Railway Setup — Step by Step

### Step 1 — Create a Railway Account

Go to railway.app. Sign up with GitHub. This connection allows Railway to access your repos.

### Step 2 — Create a New Project

Click **New Project** → **Empty Project**. Name it `hirestory`.

### Step 3 — Add PostgreSQL

Inside your project:

- Click **Add Service** → **Database** → **PostgreSQL**
- Railway creates a PostgreSQL instance immediately
- Click on it → **Variables** tab
- Copy `DATABASE_URL` — you will need this format

Railway's PostgreSQL URL looks like:

```
postgresql://postgres:password@monorail.proxy.rlwy.net:12345/railway
```

Your Spring Boot app needs it in JDBC format:

```
jdbc:postgresql://monorail.proxy.rlwy.net:12345/railway
```

### Step 4 — Add Redis

- Click **Add Service** → **Database** → **Redis**
- Railway creates a Redis instance
- Click on it → **Variables** tab
- Note `REDIS_URL` — format: `redis://:password@host:port`

### Step 5 — Add RabbitMQ

Railway does not have a built-in RabbitMQ service. Use the community template:

- Click **Add Service** → **Template**
- Search for **RabbitMQ**
- Deploy the template
- Note the connection details from its variables

Alternatively, use CloudAMQP (free tier available at cloudamqp.com) and set the URL as an environment variable.

### Step 6 — Add Your Spring Boot App

- Click **Add Service** → **GitHub Repo**
- Select your HireStory backend repository
- Railway detects the Dockerfile automatically
- Click **Deploy**

Your first deploy will fail — because environment variables are not set yet. That is expected. Do Step 7 next.

---

## 11.5 Environment Variables — The Complete List

Click on your Spring Boot service → **Variables** tab. Add every variable below. This is the most critical step.

```bash
# ── Spring Profile ────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=prod

# ── Database ──────────────────────────────────────────────────────
# Convert Railway's DATABASE_URL to JDBC format
# Railway gives:    postgresql://user:pass@host:port/dbname
# You need:         jdbc:postgresql://host:port/dbname
DB_URL=jdbc:postgresql://monorail.proxy.rlwy.net:12345/railway
DB_USERNAME=postgres
DB_PASSWORD=your_railway_postgres_password

# ── Redis ─────────────────────────────────────────────────────────
# Railway gives: redis://:password@host:port
REDIS_HOST=monorail.proxy.rlwy.net
REDIS_PORT=6380
REDIS_PASSWORD=your_railway_redis_password

# ── RabbitMQ ──────────────────────────────────────────────────────
RABBITMQ_HOST=your-rabbitmq-host
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=your_rabbitmq_password

# ── Clerk ─────────────────────────────────────────────────────────
CLERK_JWKS_URL=https://your-app.clerk.accounts.dev/.well-known/jwks.json
CLERK_WEBHOOK_SECRET=whsec_your_webhook_secret_from_clerk_dashboard

# ── OpenAI ────────────────────────────────────────────────────────
OPENAI_API_KEY=sk-proj-your_openai_api_key

# ── Firebase ──────────────────────────────────────────────────────
# Base64-encode your service account JSON:
# cat service-account.json | base64 | tr -d '\n'
FIREBASE_SERVICE_ACCOUNT_KEY=eyJ0eXBlIjoic2VydmljZV9hY2NvdW50...

# ── CORS ──────────────────────────────────────────────────────────
CORS_ALLOWED_ORIGINS=https://hirestory.com,https://www.hirestory.com

# ── App ───────────────────────────────────────────────────────────
PORT=8080
```

### Updating application.yml to Use These Variables

Your `application.yml` already uses `${VAR:default}` syntax from Chapter 1. Make sure every production variable is referenced correctly:

```yaml
# application.yml — confirm these match your env var names

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

hirestory:
  clerk:
    jwks-url: ${CLERK_JWKS_URL}
    webhook-secret: ${CLERK_WEBHOOK_SECRET}
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

---

## 11.6 Flyway in Production — Migrations Run Automatically

When your Spring Boot app starts on Railway, Flyway runs automatically. It checks which migrations have already run and applies any new ones.

This means:

- First deploy: Flyway runs `V1__create_initial_schema.sql` and `V2__create_indexes.sql` and all other migration files — creates all your tables
- Second deploy (new feature): Flyway runs only the new migration file — existing data is untouched

You never need to manually run SQL against your production database. Flyway handles it on every startup.

### Verifying Flyway Ran Successfully

After your first deploy, check the Railway logs:

```
✅ Good logs — Flyway ran successfully:
INFO  o.f.c.i.c.DbValidate - Successfully validated 3 migrations
INFO  o.f.c.i.c.DbMigrate - Current version of schema "public": << Empty Schema >>
INFO  o.f.c.i.c.DbMigrate - Migrating schema "public" to version "1 - create initial schema"
INFO  o.f.c.i.c.DbMigrate - Migrating schema "public" to version "2 - create indexes"
INFO  o.f.c.i.c.DbMigrate - Successfully applied 2 migrations

❌ Bad logs — migration file was edited after running:
ERROR o.f.c.i.command.DbValidate - Validate failed:
Migration checksum mismatch for migration version 1
→ Applied to database : 1234567890
→ Resolved locally    : 9876543210
```

If you see the checksum mismatch error, you edited a migration file that already ran. Fix: add a new `V3__fix_whatever.sql`instead of editing `V1`.

---

## 11.7 Health Checks and Readiness

Railway needs to know when your app is ready to receive traffic. Without a health check, Railway sends traffic before your app has finished starting up — users get connection errors.

Your Actuator health endpoint from Chapter 1 handles this:

```yaml
# application.yml — already configured
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

In Railway, configure the health check:

- Click your Spring Boot service → **Settings** tab
- **Health Check Path**: `/api/actuator/health`
- **Health Check Timeout**: `120` seconds (Spring Boot takes 5-20 seconds to start — give it enough time)

Railway will:

1. Start your container
2. Wait until `GET /api/actuator/health` returns `{"status":"UP"}`
3. Only then route traffic to it
4. If the app crashes, restart it automatically

### Custom Health Indicators

Add health checks for your critical dependencies:

```kotlin
// src/main/kotlin/com/example/hirestory/health/DatabaseHealthIndicator.kt

package com.example.hirestory.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component("redis")
class RedisHealthIndicator(
    private val redisTemplate: StringRedisTemplate
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val pong = redisTemplate.connectionFactory
                ?.connection
                ?.ping()
            if (pong == "PONG") {
                Health.up()
                    .withDetail("response", pong)
                    .build()
            } else {
                Health.down()
                    .withDetail("response", pong ?: "no response")
                    .build()
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message)
                .build()
        }
    }
}
```

Now `GET /api/actuator/health` returns:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP", "details": { "response": "PONG" } },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## 11.8 Continuous Deployment — Push to Deploy

Once Railway is connected to your GitHub repo, every push to `main` triggers a new deploy automatically.

The deploy sequence:

```
You push code to GitHub main branch
        ↓
Railway detects new commit (webhook)
        ↓
Railway builds new Docker image
        ↓
Railway starts new container
        ↓
Health check passes: GET /api/actuator/health → 200
        ↓
Railway routes traffic to new container
        ↓
Old container shuts down gracefully (30 second window)
        ↓
Deploy complete — zero downtime
```

The graceful shutdown from `application-prod.yml` ensures in-flight requests complete before the old container dies.

### Protect Your Main Branch

Add branch protection on GitHub so tests must pass before merging:

- GitHub → Settings → Branches → Add rule for `main`
- Check: **Require status checks to pass**
- Add your GitHub Actions workflow (below)

```yaml
# .github/workflows/ci.yml

name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: hirestory_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle.kts') }}

      - name: Run tests
        run: ./gradlew test
        env:
          SPRING_PROFILES_ACTIVE: test
```

---

## 11.9 Reading Production Logs

Railway shows your app logs in real time. Click on your service → **Logs** tab.

### What to Look For After Deploy

```bash
# ✅ Successful startup sequence:
INFO  o.f.c.i.c.DbMigrate - Successfully applied 2 migrations
INFO  o.s.d.r.c.RepositoryConfigurationDelegate - Bootstrapping Spring Data JPA
INFO  o.s.b.w.e.t.TomcatWebServer - Tomcat initialized with port 8080
INFO  c.e.h.HireStoryApplication - Started HireStoryApplication in 8.234 seconds

# ❌ Database connection failed:
ERROR o.s.b.SpringApplication - Application run failed
Caused by: java.sql.SQLException: Connection refused
→ Check DB_URL, DB_USERNAME, DB_PASSWORD env vars

# ❌ Redis connection failed:
ERROR o.s.d.r.c.RedisConnectionFailureException - Unable to connect
→ Check REDIS_HOST, REDIS_PORT, REDIS_PASSWORD env vars

# ❌ Clerk JWKS fetch failed:
ERROR c.e.h.s.JwtService - Failed to fetch Clerk public keys
→ Check CLERK_JWKS_URL env var

# ❌ Flyway checksum mismatch:
ERROR o.f.c.i.command.DbValidate - Validate failed: Migration checksum mismatch
→ You edited a migration file that already ran
```

### Filtering Logs

In the Railway logs panel, use the search box to filter:

- Type `ERROR` — see only error messages
- Type `Started` — see startup completion
- Type `Flyway` — see only migration logs
- Type `INFO c.e.h` — see only your app's log messages

---

## 11.10 Custom Domain

Railway gives you a URL like `hirestory-api.up.railway.app`. For production, point your custom domain at it:

**Step 1 — Add domain in Railway**

- Click your service → **Settings** → **Networking**
- Click **Generate Domain** for the Railway subdomain
- Click **Custom Domain** → enter `api.hirestory.com`

**Step 2 — Add DNS record** In your domain registrar (GoDaddy, Namecheap, Cloudflare):

```
Type: CNAME
Name: api
Value: hirestory-api.up.railway.app
TTL: 3600
```

**Step 3 — Update your app**

Update `CORS_ALLOWED_ORIGINS` to include your production domains. Update `CLERK_JWKS_URL` if you have a custom Clerk domain. Update the Android app's base URL to `https://api.hirestory.com`.

---

## 11.11 Environment Variable Reference — Never Lose These

Keep a `.env.example` file in your repo (never the real values):

```bash
# .env.example — copy this, fill in real values, NEVER commit the filled version

# Spring
SPRING_PROFILES_ACTIVE=prod

# Database (Railway PostgreSQL)
DB_URL=jdbc:postgresql://HOST:PORT/DBNAME
DB_USERNAME=postgres
DB_PASSWORD=

# Redis (Railway Redis)
REDIS_HOST=
REDIS_PORT=6379
REDIS_PASSWORD=

# RabbitMQ
RABBITMQ_HOST=
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=

# Clerk Authentication
CLERK_JWKS_URL=https://YOUR_APP.clerk.accounts.dev/.well-known/jwks.json
CLERK_WEBHOOK_SECRET=whsec_

# OpenAI
OPENAI_API_KEY=sk-proj-

# Firebase
# Generate: cat service-account.json | base64 | tr -d '\n'
FIREBASE_SERVICE_ACCOUNT_KEY=

# CORS
CORS_ALLOWED_ORIGINS=https://hirestory.com

# Server
PORT=8080
```

---

## 11.12 Production Readiness Checklist

Go through every item before calling your backend production-ready:

### Database

- [ ] Flyway migrations run successfully on first deploy
- [ ] `ddl-auto: validate` — never `create` or `create-drop`
- [ ] Database backup enabled in Railway (Settings → Backups)
- [ ] Connection pool configured (`hikari.maximum-pool-size: 10`)

### Security

- [ ] `SPRING_PROFILES_ACTIVE=prod` set in Railway
- [ ] All sensitive values in environment variables — nothing hardcoded
- [ ] CORS allows only your actual domains — not `*`
- [ ] Clerk webhook signature verification is active
- [ ] Admin endpoints protected by `hasRole('ADMIN')`
- [ ] Actuator exposes only `health` and `info` — not all endpoints

### Performance

- [ ] `show_sql: false` in production profile
- [ ] Redis caching active — verify cache hit rate in logs
- [ ] `open-in-view: false` — set from Chapter 1

### Reliability

- [ ] Health check configured in Railway
- [ ] Graceful shutdown configured (`server.shutdown: graceful`)
- [ ] RabbitMQ DLQ configured — failed messages are not lost
- [ ] `CrawlRecoveryScheduler` running — stuck jobs are recovered
- [ ] All external calls (Firebase, OpenAI, Clerk) have try-catch

### Observability

- [ ] Logging level `INFO` for your package in production
- [ ] Error logs include full stack traces
- [ ] Unhandled exceptions caught by `GlobalExceptionHandler`
- [ ] Health check endpoint returns dependency status

### Deployment

- [ ] Docker build succeeds locally before pushing
- [ ] GitHub Actions CI runs tests on every push
- [ ] Branch protection enabled on `main`
- [ ] `.env.example` in repo — no real secrets committed
- [ ] `.gitignore` excludes `application-local.yml`, `.env`

---

## 11.13 Verifying Your Production Deployment

After your first successful deploy, verify these manually:

```bash
# Replace with your actual Railway URL
BASE=https://hirestory-api.up.railway.app/api

# 1. Health check
curl $BASE/actuator/health
# Expected: {"status":"UP"}

# 2. Feed endpoint (public)
curl $BASE/interviews
# Expected: {"content":[],"hasNext":false,"page":0,"size":20}

# 3. Company endpoint (public)
curl $BASE/companies
# Expected: [] (empty until you seed data)

# 4. Protected endpoint without token
curl $BASE/profile
# Expected: {"status":401,"error":"Unauthorized",...}

# 5. Admin endpoint without token
curl $BASE/admin/stats
# Expected: {"status":401,"error":"Unauthorized",...}

# 6. Invalid JSON body
curl -X POST $BASE/interviews \
  -H "Content-Type: application/json" \
  -d '{ invalid json }'
# Expected: {"status":400,"error":"Malformed Request",...}
```

All six must return exactly what you expect. If any of them fail, check Railway logs for the error.

---

## 11.14 Seeding Production Data

Your `DataInitializer` is annotated with `@Profile("dev")` — it will not run in production. Good. You do not want test data appearing in your live app.

For production, seed real companies through the admin API:

```bash
# First, get your admin user's JWT token from Clerk dashboard
ADMIN_TOKEN="eyJhbGc..."

# Seed companies via your API
curl -X POST https://hirestory-api.up.railway.app/api/admin/companies \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Google", "logoUrl": "https://logo.clearbit.com/google.com"}'

curl -X POST https://hirestory-api.up.railway.app/api/admin/companies \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Amazon", "logoUrl": "https://logo.clearbit.com/amazon.com"}'
```

Then trigger the crawler to start filling content:

```bash
curl -X POST https://hirestory-api.up.railway.app/api/admin/crawler/trigger \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Expected: {"queued": 47, "message": "Discovered: 62, Queued: 47..."}
```

---

## 11.15 Common Mistakes in Chapter 11

### Mistake 1 — Committing secrets to Git

```bash
# ❌ Real credentials in application.yml — pushed to GitHub
spring:
  datasource:
    password: myRealPassword123   # Now permanently in Git history

# ✅ Environment variable — nothing sensitive in code
spring:
  datasource:
    password: ${DB_PASSWORD}
```

If you accidentally commit a secret, you must:

1. Rotate the secret immediately (change the password, regenerate the API key)
2. Remove from Git history with `git filter-branch` or BFG Repo Cleaner
3. Force push (requires team coordination) The secret is compromised the moment it is pushed — rotation is not optional.

### Mistake 2 — ddl-auto: create in production

```yaml
# ❌ This deletes ALL your production data on every restart
spring:
  jpa:
    hibernate:
      ddl-auto: create

# ✅ Flyway manages schema — Hibernate only validates
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

### Mistake 3 — No health check configured in Railway

```
❌ Without health check:
Railway starts your container
Railway immediately sends traffic
App is still initializing (Flyway running, beans loading)
Users get "Connection Refused" for the first 10-20 seconds

✅ With health check:
Railway starts your container
Railway waits until /actuator/health returns 200
Then routes traffic
Users always hit a ready app
```

### Mistake 4 — Forgetting to set SPRING_PROFILES_ACTIVE

```bash
# ❌ Without this, Spring uses application.yml only
# show_sql: true in production, debug logging everywhere
# Potentially dangerous ddl-auto setting

# ✅ Set this first in Railway environment variables
SPRING_PROFILES_ACTIVE=prod
```

### Mistake 5 — Using Railway's DATABASE_URL directly

```bash
# Railway gives you:
DATABASE_URL=postgresql://postgres:pass@host:port/railway

# ❌ Spring Boot needs JDBC format — using Railway's URL directly fails
spring.datasource.url=${DATABASE_URL}
# Results in: No suitable driver found for postgresql://...

# ✅ Convert to JDBC format in Railway variables
DB_URL=jdbc:postgresql://host:port/railway
```

### Mistake 6 — Deploying without running tests

```bash
# ❌ Push straight to main — broken code goes live
git add .
git commit -m "fix"
git push origin main

# ✅ Tests run in CI before deploy reaches production
# If tests fail: Railway deploy is NOT triggered
# Branch protection + GitHub Actions = safety net
```

---

## 11.16 Monitoring After Launch

Once live, watch these metrics daily for the first two weeks:

**Railway dashboard:**

- CPU usage — should be low except during crawler runs
- Memory usage — should be stable, not growing (memory leak indicator)
- Deploy frequency — how often you are shipping

**Your logs:**

- `ERROR` count — should be near zero
- Average response time — should be under 200ms for cached endpoints
- Crawler run results — how many new jobs queued per run

**Business metrics (check via admin endpoint):**

- Total published interviews — growing?
- Extraction success rate — above 60%?
- Average confidence score — above 65%?

---

## 11.17 HireStory Connection — What You Built in Chapter 11

By the end of Chapter 11, HireStory's backend is live:

- **Dockerfile** — two-stage build, non-root user, JVM container tuning
- **application-prod.yml** — no SQL logging, graceful shutdown, `ddl-auto: validate`, quiet log levels
- **Railway project** — PostgreSQL + Redis + RabbitMQ + Spring Boot app, all connected on private network
- **Environment variables** — every secret safely outside of code
- **Flyway** — runs all migrations automatically on first deploy, applies only new migrations on subsequent deploys
- **Health check** — Railway waits for `UP` before routing traffic
- **Graceful shutdown** — in-flight requests complete before container dies
- **GitHub Actions CI** — tests run on every push, broken code never deploys
- **Custom domain** — `api.hirestory.com` pointing to Railway
- **Production checklist** — every item verified before launch

Your backend is now:

- Accessible from the internet at a stable URL
- Redeploying automatically on every push to `main`
- Restarting automatically if it crashes
- Running Flyway migrations safely on every deploy
- Serving real users with cached responses under 10ms

---

## 11.18 Chapter Project — Deploy Before You Move On

This chapter project has one goal: your app must be live on Railway and passing all six verification checks from Section 11.13.

**Step 1 — Write the Dockerfile**

Copy from Section 11.3.2. Build it locally. Run it locally. Verify `GET localhost:8080/api/actuator/health` returns `UP`.

**Step 2 — Create the production profile**

Copy `application-prod.yml` from Section 11.3.1. Make sure `show_sql: false` and `ddl-auto: validate`.

**Step 3 — Set up Railway**

Create account. Create project. Add PostgreSQL, Redis, RabbitMQ. Connect your GitHub repo. Set ALL environment variables from Section 11.5.

**Step 4 — Watch the first deploy**

Open Railway logs. Watch Flyway run. Watch the app start. Look for `Started HireStoryApplication in X seconds`.

**Step 5 — Configure health check**

Add `/api/actuator/health` as health check path in Railway settings. Trigger a new deploy. Watch Railway wait for health check before routing.

**Step 6 — Run the six verification checks**

Run every `curl` command from Section 11.13. All six must return the expected responses. If any fail, check the logs, fix the issue, redeploy.

**Step 7 — Push a small change**

Edit one log message in your code. Push to `main`. Watch Railway automatically build and deploy. Verify the change is live without any manual steps.

### Checkpoint questions — answer before moving on

1. Your app starts but Flyway throws: `FlywayException: Validate failed: Migration checksum mismatch for version 1` What happened and what is the correct fix? (There are two possible causes — name both.)
    
2. Railway shows your deploy as successful but `GET /api/actuator/health` returns `{"status":"DOWN"}`. What does this tell you? List three things you would check first.
    
3. A user reports that immediately after you deploy a new version, they get errors for about 30 seconds. You have graceful shutdown configured. What is the likely cause and how do you fix it?
    
4. You need to add a new column `website_url` to the `companies` table. Walk through the exact steps from writing the code to it being live in production without any downtime or data loss.
    
5. Your production logs show this error every 5 minutes: `ERROR c.e.h.s.CacheService - Redis unavailable: Connection refused`The app is still serving requests. What is happening, why is the app still working, and what is the correct action to take?
    

---

## 11.19 What You Have Built — The Complete Backend

You have now built a complete, production-grade Spring Boot backend. Let us look at what exists:

```
HireStory Backend — fully deployed
│
├── API Layer (Chapter 4)
│   ├── InterviewController  — feed, detail, submit, search
│   ├── CompanyController    — list, detail, interviews
│   ├── BookmarkController   — add, remove, list
│   ├── ProfileController    — user profile, read stats
│   ├── NotificationController — list, mark read, register FCM token
│   ├── AdminController      — review queue, approve, reject, stats
│   ├── WebhookController    — Clerk user events
│   └── GlobalExceptionHandler — consistent JSON errors for everything
│
├── Security (Chapter 5)
│   ├── JwtAuthenticationFilter — validates Clerk tokens on every request
│   ├── SecurityConfig          — public vs protected vs admin endpoints
│   └── ReadTrackingService     — free tier enforcement (25 reads/month)
│
├── Database (Chapters 2, 3)
│   ├── 14 tables via Flyway migrations
│   ├── All JPA entities with correct relationships
│   └── 8 repositories with custom queries, pagination, full-text search
│
├── Caching (Chapter 6)
│   ├── Feed cache — 5 minute TTL per filter combination
│   ├── Company cache — 1 hour TTL
│   ├── Read counter — Redis increment per user per month
│   └── URL dedup — Redis SETNX for crawler
│
├── Async Processing (Chapter 7)
│   ├── NotificationPublisher — 5 notification types
│   ├── NotificationConsumer  — FCM delivery + database storage
│   ├── CrawlConsumer         — AI extraction pipeline
│   └── OnboardingScheduler   — day 1/3/7 notification sequences
│
├── Content Pipeline (Chapters 8, 9)
│   ├── RedditCrawler    — searches 3 subreddits
│   ├── GfgCrawler       — scrapes interview experiences
│   ├── DevToCrawler     — fetches tagged articles
│   ├── AiExtractionService — GPT-4o Mini structured extraction
│   └── ExtractionProcessor — confidence-based auto-publish
│
├── Testing (Chapter 10)
│   ├── Unit tests for all services
│   ├── Repository tests with @DataJpaTest
│   └── Controller tests with @WebMvcTest
│
└── Deployment (Chapter 11)
    ├── Dockerfile — two-stage build
    ├── Railway — PostgreSQL + Redis + RabbitMQ + App
    ├── Flyway — automatic migrations on every deploy
    └── GitHub Actions — CI runs tests before deploy
```

This is a real, production-quality backend. Not a tutorial project — a deployable product.

---

_The backend is complete._ _Chapters 12–15 cover KMP shared module, Android UI, Next.js web, and Play Store submission._

---

> **Book Progress: Chapter 11 of 11 backend chapters complete.** Your Spring Boot + Kotlin backend is built, tested, and deployed.