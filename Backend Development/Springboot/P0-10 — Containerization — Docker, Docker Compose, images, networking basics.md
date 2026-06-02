# P0-10 — Containerization: Docker, Docker Compose, Images, Networking Basics

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Production Truth**: "It works on my machine" killed more startups than bad product-market fit. Docker solves the environment parity problem once and for all. But Docker in production is only the beginning — you need to understand images, layers, networking, secrets management, and exactly when to graduate to Kubernetes.

---

## Why Containers: The Core Problem They Solve

Before containers, deploying a Spring Boot application looked like this:
1. Install Java 17 on the server (but Java 11 is already installed for another app)
2. Configure environment variables manually on each server
3. Install PostgreSQL client libraries
4. Set up systemd service files
5. Configure log rotation
6. Pray that the server OS matches your developer's OS

**The result**: "It works on my machine" became "works in staging, breaks in production" because the JVM version differed by a patch release, or an OS library had a slightly different behavior, or an environment variable was missing on one of 10 servers.

Containers package your application **and its entire runtime environment** into a single, immutable, portable unit. Every environment — your laptop, CI/CD, staging, production — runs the **exact same bit-for-bit image**. No more environment drift.

### The Three Things Containers Give You

| Property | Meaning | Production Value |
|----------|---------|-----------------|
| **Environment Parity** | Same image runs everywhere | Eliminates "works on my machine" |
| **Reproducibility** | Image is immutable — tag `v2.4.1` always means the same thing | Reliable rollbacks |
| **Isolation** | Container has its own filesystem, network namespace, process space | Multiple apps on same host without conflicts |

---

## Docker Fundamentals: Images, Containers, Layers

### The Mental Model

```
Dockerfile     → Build instruction set
Image          → Immutable snapshot built from Dockerfile (like a class)
Container      → Running instance of an image (like an object)
Registry       → Storage for images (Docker Hub, AWS ECR, Google GCR, GitHub GHCR)
```

### How Docker Layers Work

Every instruction in a Dockerfile creates a new **layer**. Layers are immutable and cached. This is the most important concept for optimizing Docker build speed.

```
Layer 5: COPY app.jar /app/             [changes per build — cannot cache]
Layer 4: RUN apt-get install curl       [changes rarely — cache hit]
Layer 3: ENV JAVA_OPTS="-Xms256m"      [changes rarely — cache hit]  
Layer 2: RUN java -version              [changes rarely — cache hit]
Layer 1: FROM eclipse-temurin:21-jre    [base image — almost never changes]
```

When you rebuild, Docker only rebuilds layers that changed AND all layers after it. This is why **layer order matters enormously**:

```dockerfile
# BAD — copies application code early, invalidating all subsequent layers on every build
FROM eclipse-temurin:21-jre
COPY . /app/              # ← This changes EVERY build. Everything after this = no cache.
RUN apt-get install curl  # ← Always rebuilt even though it never changes
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# GOOD — stable layers first, volatile layers last
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*  # Cached
ENV JAVA_OPTS="-Xms256m -Xmx512m"  # Cached
EXPOSE 8080
COPY app.jar /app/app.jar  # Only this layer and below rebuild when app changes
ENTRYPOINT ["java", $JAVA_OPTS, "-jar", "/app/app.jar"]
```

> [!IMPORTANT]
> Layer caching is what makes Docker builds fast or slow in CI/CD. A properly structured Dockerfile builds in 10 seconds on cache hit. A poorly structured one rebuilds everything every time — 5+ minutes. At Netflix scale, this means the difference between 5-minute and 30-minute deployment pipelines.

---

## The Production Dockerfile for Spring Boot

### Why Multi-Stage Builds Are Non-Negotiable

A single-stage build would put your JDK, Gradle/Maven, source code, test dependencies, and everything else into the final image. That's a 1.5GB+ image. A multi-stage build produces a lean runtime image:

```
Stage 1 (builder): JDK + Gradle + source code = 1.2GB
Stage 2 (runtime): JRE + compiled JAR only   = 180MB
```

The runtime image is what actually gets deployed. 180MB vs 1.2GB means:
- Faster image pulls in Kubernetes (especially at scale with 100 pods)
- Smaller attack surface (JDK tools like `jshell`, `javac` not present)
- Faster container startup in auto-scaling events

```dockerfile
# ─── Stage 1: Build Stage ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /build

# Copy only dependency-related files first (cache optimization)
# If only source code changed, these layers are still cached
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Download dependencies — this layer is cached until build.gradle.kts changes
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Now copy source code (changes most frequently — so comes LAST)
COPY src/ src/

# Build the application — skip tests in Docker build (tests run in CI before this)
RUN ./gradlew bootJar --no-daemon -x test

# ─── Stage 2: Extract JAR layers (Spring Boot layered JAR) ──────────────────
# Spring Boot 2.3+ supports layered JARs for better Docker layer caching
FROM eclipse-temurin:21-jdk-alpine AS extractor
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/build/libs/*.jar app.jar

# Extract JAR into layers: dependencies, spring-boot-loader, snapshot-dependencies, application
RUN java -Djarmode=layertools -jar app.jar extract

# ─── Stage 3: Runtime Stage ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: don't run as root
# Most container security scanners (Trivy, Snyk) will flag root-running containers
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser

WORKDIR /app

# Copy layered JAR contents from extractor stage
# Order: least-to-most frequently changing (maximizes Docker layer cache hits)
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

# Install curl for health check (minimal tool set only)
RUN apk add --no-cache curl

# Switch to non-root user
USER appuser

# JVM tuning for containers
# UseContainerSupport: Makes JVM respect cgroup memory limits (critical for Kubernetes)
# MaxRAMPercentage: Use 75% of container memory for heap, leave 25% for off-heap/OS
# ExitOnOutOfMemoryError: Crash fast on OOM rather than limping along in a degraded state
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.output.ansi.enabled=NEVER"

# Expose application port
EXPOSE 8080

# Health check: Docker will mark container unhealthy if this fails
# start_period: Give app 60s to start before health checks count
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

# Use exec form (not shell form) so signals are passed directly to JVM
# Shell form: ENTRYPOINT ["sh", "-c", "java ..."] — JVM gets SIGTERM indirectly, slow shutdown
# Exec form: ENTRYPOINT ["java", "..."] — JVM gets SIGTERM directly, graceful shutdown works
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

> [!WARNING]
> **`UseContainerSupport` is critical in Kubernetes**. Without it, the JVM reads the host machine's total RAM (e.g., 64GB on a node) and allocates heap accordingly, even if your container's memory limit is 512MB. The JVM will then use 48GB of heap in a 512MB container, causing immediate OOMKill. This is a real bug that takes down entire Kubernetes nodes.

### .dockerignore — What NOT to Include

```
# .dockerignore
# Build outputs
build/
out/
target/
.gradle/
.idea/

# OS and editor files
.DS_Store
*.swp
*.swo
.env*

# Git history (no need in image)
.git/
.gitignore

# Documentation and non-essential files
README.md
*.md
docs/

# CI/CD configuration
.github/
.circleci/
Jenkinsfile

# Docker files themselves
Dockerfile*
docker-compose*.yml

# Test files (don't include test resources in production image)
src/test/

# Local configuration
application-local.yml
application-local.properties
```

Without `.dockerignore`, `COPY . .` copies everything including `.git/` history, your IDE `.idea/` directory (300MB+), build outputs, and local config files into the image. This is a massive security and size problem.

---

## Spring Boot Gradle Plugin: bootBuildImage (Buildpacks)

If you don't want to write and maintain a Dockerfile, Spring Boot's Gradle plugin integrates with [Cloud Native Buildpacks](https://buildpacks.io/) to produce OCI-compliant images automatically:

```kotlin
// build.gradle.kts
plugins {
    id("org.springframework.boot") version "3.2.0"
}

tasks.bootBuildImage {
    imageName.set("your-registry.com/${project.name}:${project.version}")
    
    // Use a specific builder (Paketo is the default and excellent)
    builder.set("paketobuildpacks/builder-jammy-base:latest")
    
    // Configure JVM settings via environment
    environment.set(mapOf(
        "BP_JVM_VERSION" to "21",
        "BP_JVM_TYPE" to "JRE",  // Only JRE in final image (not full JDK)
        "BPE_JAVA_TOOL_OPTIONS" to "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
    ))
    
    // Push directly to registry after build
    publish.set(false) // Set true to push in CI
    
    // Docker settings for pushing to private registry
    docker {
        publishRegistry {
            username.set(System.getenv("REGISTRY_USERNAME"))
            password.set(System.getenv("REGISTRY_PASSWORD"))
            url.set("https://your-registry.com")
        }
    }
}
```

```bash
# Build and push image
./gradlew bootBuildImage --publishImage
```

**Buildpacks vs Custom Dockerfile**:
- **Buildpacks**: Zero maintenance, automatic security patches, best practices built in. Best for standard Spring Boot apps.
- **Custom Dockerfile**: Full control. Required for custom runtime configuration, specific base images, non-standard setups.

---

## Docker Compose for Local Development

Docker Compose defines your entire local development environment as code. Every developer on your team runs `docker compose up` and gets identical environments.

```yaml
# docker-compose.yml — Complete local development stack
version: '3.9'

networks:
  app-network:
    driver: bridge
    # All services can reach each other by service name (DNS resolution)
    # payment-service can connect to postgres:5432 (not localhost:5432)

volumes:
  postgres-data:      # Persists between container restarts
  redis-data:
  kafka-data:
  zookeeper-data:

services:

  # ─── PostgreSQL ───────────────────────────────────────────────────────────
  postgres:
    image: postgres:16-alpine
    container_name: postgres
    restart: unless-stopped
    networks:
      - app-network
    ports:
      - "5432:5432"   # Expose to host for connecting with DBeaver/psql
    environment:
      POSTGRES_DB: appdb
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD: ${DB_PASSWORD:-localpassword}  # From .env file or default
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --locale=C"
    volumes:
      - postgres-data:/var/lib/postgresql/data  # Persist data across restarts
      - ./sql/init:/docker-entrypoint-initdb.d:ro  # Run SQL scripts on first startup
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d appdb"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    command: >
      postgres
        -c shared_buffers=256MB
        -c work_mem=4MB
        -c max_connections=100
        -c log_min_duration_statement=1000  # Log queries >1s in local dev

  # ─── Redis ────────────────────────────────────────────────────────────────
  redis:
    image: redis:7.2-alpine
    container_name: redis
    restart: unless-stopped
    networks:
      - app-network
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
      - ./redis/redis.conf:/etc/redis/redis.conf:ro
    command: redis-server /etc/redis/redis.conf
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

  # ─── Kafka ────────────────────────────────────────────────────────────────
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.3
    container_name: zookeeper
    networks:
      - app-network
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper-data:/var/lib/zookeeper/data

  kafka:
    image: confluentinc/cp-kafka:7.5.3
    container_name: kafka
    networks:
      - app-network
    ports:
      - "9092:9092"       # External access from host
      - "29092:29092"     # Internal access from other containers
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    volumes:
      - kafka-data:/var/lib/kafka/data
    depends_on:
      - zookeeper
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:29092"]
      interval: 30s
      timeout: 10s
      retries: 5

  # ─── NGINX (Reverse Proxy / API Gateway for local) ───────────────────────
  nginx:
    image: nginx:1.25-alpine
    container_name: nginx
    restart: unless-stopped
    networks:
      - app-network
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      payment-service:
        condition: service_healthy
      order-service:
        condition: service_healthy

  # ─── Spring Boot: Payment Service ─────────────────────────────────────────
  payment-service:
    build:
      context: ./payment-service
      dockerfile: Dockerfile
      target: runtime  # Use the runtime stage from multi-stage build
      cache_from:
        - your-registry.com/payment-service:latest  # Pull remote image for layer cache
    image: payment-service:local
    container_name: payment-service
    restart: unless-stopped
    networks:
      - app-network
    ports:
      - "8081:8080"   # Expose on 8081 (nginx routes /api/v1/payments here on port 80)
      - "9091:9090"   # Actuator on separate port
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/appdb
      SPRING_DATASOURCE_USERNAME: appuser
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-localpassword}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      RAZORPAY_KEY_ID: ${RAZORPAY_KEY_ID}
      RAZORPAY_KEY_SECRET: ${RAZORPAY_KEY_SECRET}
      # JVM settings
      JAVA_OPTS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xms128m"
    # Resource limits for local dev (prevents one service from eating all RAM)
    deploy:
      resources:
        limits:
          memory: 512m
          cpus: '1.0'
        reservations:
          memory: 256m
    depends_on:
      postgres:
        condition: service_healthy   # Don't start until DB is ready (not just running)
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "--fail", "--silent", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      start_period: 60s  # Spring Boot needs time to start
      retries: 3
    volumes:
      # Mount local config for hot-reloading in development (use spring-boot-devtools)
      - ./payment-service/src/main/resources/application-local.yml:/app/BOOT-INF/classes/application-local.yml:ro

  # ─── Spring Boot: Order Service ───────────────────────────────────────────
  order-service:
    build:
      context: ./order-service
      dockerfile: Dockerfile
      target: runtime
    image: order-service:local
    container_name: order-service
    restart: unless-stopped
    networks:
      - app-network
    ports:
      - "8082:8080"
      - "9092:9090"
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/appdb
      SPRING_DATASOURCE_USERNAME: appuser
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-localpassword}
      PAYMENT_SERVICE_URL: http://payment-service:8080  # Service name DNS resolution
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    deploy:
      resources:
        limits:
          memory: 512m
    depends_on:
      postgres:
        condition: service_healthy
      payment-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "--fail", "--silent", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      start_period: 60s
      retries: 3
```

### .env File for Local Development

```bash
# .env — LOCAL DEVELOPMENT ONLY. NEVER COMMIT THIS FILE TO GIT.
# Add .env to .gitignore immediately.

# Database
DB_PASSWORD=localpassword_dev_only

# Razorpay (use test mode keys for local)
RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxx
RAZORPAY_KEY_SECRET=xxxxxxxxxxxxxxxxxx

# Redis password (if configured)
REDIS_PASSWORD=

# Grafana
GRAFANA_USER=admin
GRAFANA_PASSWORD=admin

# JWT secret (for local dev only — production uses a proper secret manager)
JWT_SECRET=local-dev-secret-replace-in-production
```

> [!CAUTION]
> **NEVER commit `.env` files to Git**. I've seen engineers at startups commit AWS access keys to public GitHub repos. The automated bots that scan GitHub for secrets found them within 4 minutes and spun up $50,000 worth of EC2 GPU instances for cryptocurrency mining. Always add `.env`, `*.env`, `application-local.*` to `.gitignore` before the first commit.

---

## Docker Networking: Service Name DNS Resolution

This is the most common confusion for Docker beginners. Understanding this saves hours of debugging.

### Bridge Network and Automatic DNS

When services are on the same Docker Compose network, Docker provides automatic DNS resolution using service names:

```
payment-service container can connect to:
  postgres:5432           ← service name, NOT localhost:5432
  redis:6379              ← service name
  kafka:29092             ← service name (internal port)
  order-service:8080      ← service name
  
Host machine can connect to:
  localhost:5432          ← mapped via `ports: "5432:5432"`
  localhost:6379          ← mapped via `ports: "6379:6379"`
  localhost:8081          ← payment-service exposed port
```

```kotlin
// application-local.yml in your Spring Boot service
spring:
  datasource:
    # When running IN Docker Compose:
    url: jdbc:postgresql://postgres:5432/appdb
    # When running OUTSIDE Docker (IntelliJ, gradle run):
    # url: jdbc:postgresql://localhost:5432/appdb
    # Use Spring profiles to switch between these configurations
```

### Network Isolation Strategy

```yaml
# Multiple networks for security isolation
networks:
  frontend-network:
    # nginx, api-gateway
  backend-network:
    # spring boot services, inter-service communication
  data-network:
    # postgres, redis, kafka — isolated from internet-facing services

services:
  nginx:
    networks:
      - frontend-network
      - backend-network  # nginx can reach backend services

  payment-service:
    networks:
      - backend-network
      - data-network     # Can reach DB and cache

  postgres:
    networks:
      - data-network     # CANNOT be reached from frontend-network
                         # Extra security: database not accessible from internet-facing containers
```

> [!NOTE]
> Docker networking has four modes: bridge (default, containers on same host talk via virtual network), host (container uses host's network stack — avoid in production), overlay (multi-host networking for Docker Swarm/Kubernetes), none (no network). For local development, bridge is always correct.

---

## Secrets Management: Never Bake Credentials into Images

### The Problem: What's in Your Image?

```dockerfile
# CATASTROPHICALLY BAD — credentials baked into image layer
ENV DB_PASSWORD=supersecret123
ENV JWT_SECRET=myjwtsecret

# These are now permanently embedded in the image layer
# Anyone who can pull the image can extract them with:
# docker history payment-service:v1.0 --no-trunc
# docker run --rm payment-service:v1.0 env
```

Even if you add another layer `ENV DB_PASSWORD=""`, the original value is still in the layer history. Docker images are permanent records.

### The Correct Approach: Runtime Injection

```yaml
# docker-compose.yml — secrets injected at runtime, not build time
services:
  payment-service:
    environment:
      # These are resolved from the host environment or .env file at `docker compose up`
      # The value is NEVER stored in the image
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      RAZORPAY_KEY_SECRET: ${RAZORPAY_KEY_SECRET}
      JWT_SECRET: ${JWT_SECRET}
```

### Docker Secrets (for Swarm/Production)

```yaml
# docker-compose.yml with Docker secrets
services:
  payment-service:
    secrets:
      - db_password
      - jwt_secret
    environment:
      # Spring Boot can read from a file instead of env variable
      SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/db_password
      JWT_SECRET_FILE: /run/secrets/jwt_secret

secrets:
  db_password:
    file: ./secrets/db_password.txt   # File-based (local dev)
    # OR for Docker Swarm production:
    # external: true  # Secret managed by Docker Swarm secret store
  jwt_secret:
    file: ./secrets/jwt_secret.txt
```

```kotlin
// Spring Boot — reading secrets from files (Docker secrets pattern)
// application.yml
spring:
  datasource:
    password: ${SPRING_DATASOURCE_PASSWORD:${SPRING_DATASOURCE_PASSWORD_FILE_CONTENT:localpassword}}
```

### Production Secret Management

| Environment | Secret Storage |
|-------------|---------------|
| Local Dev | `.env` file (never committed) |
| Docker Compose (staging) | Environment variables injected by CI |
| Kubernetes | Kubernetes Secrets + External Secrets Operator → AWS Secrets Manager |
| AWS ECS | AWS Secrets Manager via task definition secrets |
| Google Cloud Run | Google Secret Manager via environment reference |

---

## Volume Mounts: Persistence and Performance

### PostgreSQL Data Persistence

```yaml
volumes:
  postgres-data:
    driver: local
    # By default, Docker stores this in /var/lib/docker/volumes/
    # For local dev on Mac, Docker Desktop uses a VM — the volume lives inside the VM

services:
  postgres:
    volumes:
      # Named volume: survives `docker compose down` but NOT `docker compose down -v`
      - postgres-data:/var/lib/postgresql/data
      
      # Bind mount: init scripts executed on first container creation
      - ./sql/migrations:/docker-entrypoint-initdb.d:ro
```

```
docker compose down        → Stops containers, PRESERVES volumes (data safe)
docker compose down -v     → Stops containers, DELETES volumes (data gone — nuclear option)
docker compose down --rmi all -v → Full reset: removes containers, images, volumes
```

### Application Config Hot-Reload Mount

During development, you can mount config files into a running container:

```yaml
services:
  payment-service:
    volumes:
      # Mount local application-local.yml into container
      # If Spring Boot DevTools is enabled, it hot-reloads config changes
      - ./src/main/resources/application-local.yml:/app/BOOT-INF/classes/application-local.yml
```

---

## Image Optimization Checklist

| Optimization | Technique | Size Impact |
|-------------|-----------|-------------|
| Use Alpine base | `eclipse-temurin:21-jre-alpine` vs `eclipse-temurin:21-jre` | -200MB |
| Multi-stage build | Only copy JAR to runtime stage | -500MB (no JDK, build tools) |
| Remove APK cache | `apk add --no-cache curl` | -10MB |
| `.dockerignore` | Exclude `.git`, `build/`, `.idea/` | -500MB+ |
| Spring Boot layered JARs | Split JAR into dependency/application layers | Faster rebuilds |
| Non-root user | Security scanner compliance | No size impact |
| `--no-daemon` for Gradle | Don't start Gradle daemon in container | No size impact |

### Inspecting Image Size

```bash
# Check image layers and sizes
docker history payment-service:latest --no-trunc

# Dive tool for interactive layer exploration
docker run --rm -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  wagoodman/dive:latest payment-service:latest

# Check final image size
docker images payment-service
```

---

## Health Checks with `depends_on`

The most common Docker Compose mistake: thinking `depends_on` waits for a service to be **ready**. By default, it only waits for the container to **start** (which takes ~1 second). PostgreSQL takes 10-30 seconds to fully initialize.

```yaml
# WRONG: service starts before postgres is actually ready for connections
services:
  payment-service:
    depends_on:
      - postgres  # Only waits for container to START, not for postgres to be READY

# CORRECT: use condition: service_healthy
services:
  payment-service:
    depends_on:
      postgres:
        condition: service_healthy  # Waits for postgres HEALTHCHECK to pass
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
  
  postgres:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d appdb"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 30s
```

> [!WARNING]
> Even with `condition: service_healthy`, your Spring Boot app should implement retry logic for database connections. Docker networks can have transient issues, and `pg_isready` returning true doesn't guarantee all postgres subsystems are fully initialized. Configure Spring Boot retry on startup with:
> ```yaml
> spring:
>   datasource:
>     hikari:
>       initialization-fail-timeout: 60000  # Wait up to 60s for initial connection
> ```

---

## NGINX Configuration for Local Development

```nginx
# nginx/conf.d/default.conf
upstream payment-service {
    server payment-service:8080;
}

upstream order-service {
    server order-service:8080;
}

server {
    listen 80;
    server_name localhost;

    # Route payment API calls to payment service
    location /api/v1/payments {
        proxy_pass http://payment-service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Correlation-ID $request_id;  # Inject correlation ID
        
        # Timeouts
        proxy_connect_timeout 5s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # Route order API calls to order service
    location /api/v1/orders {
        proxy_pass http://order-service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Correlation-ID $request_id;
    }

    # Health check endpoint (returns 200 OK without touching backends)
    location /health {
        access_log off;
        return 200 'healthy\n';
        add_header Content-Type text/plain;
    }
}
```

---

## Docker Compose Commands Cheat Sheet

```bash
# Start all services in background
docker compose up -d

# Start specific service and its dependencies
docker compose up -d payment-service

# View logs from all services (follow mode)
docker compose logs -f

# View logs from specific service
docker compose logs -f payment-service

# View last 100 lines from payment service
docker compose logs --tail=100 payment-service

# Execute command inside running container
docker compose exec postgres psql -U appuser -d appdb

# Execute bash inside payment service
docker compose exec payment-service sh

# Restart specific service (after code change)
docker compose restart payment-service

# Rebuild and restart specific service
docker compose up -d --build payment-service

# Check status of all services
docker compose ps

# Check resource usage
docker stats

# Stop all services (keep volumes)
docker compose down

# Stop all services and remove volumes (clean slate)
docker compose down -v

# Pull latest images
docker compose pull

# Scale a service to N instances (requires different port handling)
docker compose up -d --scale payment-service=3
```

---

## Docker Compose vs Kubernetes: When to Upgrade

> **This is the question every startup asks at the wrong time.**

```
Stage 1: Prototype / MVP
→ Docker Compose on a single VM
→ Simple, fast, easy to debug
→ Appropriate for: <100 RPS, 1-3 developers

Stage 2: Growing Product  
→ Docker Compose on multiple VMs (manually managed) OR
→ AWS ECS / Google Cloud Run (managed container orchestration)
→ Appropriate for: 100-1000 RPS, 5-15 developers, 2-5 services

Stage 3: Scale
→ Kubernetes (EKS/GKE/AKS)
→ Appropriate for: 1000+ RPS, 15+ developers, 10+ services
→ Justification: Auto-scaling, self-healing, zero-downtime deploys, multi-zone HA
```

> [!IMPORTANT]
> **Do not go to Kubernetes prematurely**. Kubernetes has a 3-6 month learning curve and requires dedicated DevOps resources. Blinkit (formerly Grofers) ran on Docker Compose and bare EC2 for 3 years before migrating to Kubernetes. Moving too early wastes engineering bandwidth that should go into product. Use managed services (AWS ECS Fargate, Google Cloud Run) as your bridge — they give you container orchestration without the operational complexity.

### What Kubernetes Adds Over Docker Compose

| Capability | Docker Compose | Kubernetes |
|-----------|---------------|------------|
| Auto-scaling | ❌ Manual | ✅ HPA (CPU/memory/custom metrics) |
| Self-healing | ❌ Manual restart | ✅ Auto-restart, reschedule on node failure |
| Rolling updates | ❌ Manual | ✅ Zero-downtime deployments |
| Multi-zone HA | ❌ Single host | ✅ Spread across availability zones |
| Service mesh | ❌ | ✅ Istio/Linkerd for mTLS, circuit breaking |
| Secret management | Basic | ✅ Kubernetes Secrets + External Secrets Operator |
| Load balancing | ❌ Basic | ✅ Built-in + AWS ALB/GCP Load Balancer integration |

---

## Production: Docker Compose Isn't for Production

To be explicit: **Docker Compose was designed for development workflows**. Using it in production means:
- Single point of failure (one VM goes down, everything goes down)
- No auto-scaling
- No zero-downtime deploys
- No cross-host networking
- No built-in rolling updates

For production, the progression is:
```
Development:      docker compose up -d
CI Build:         docker build + docker push to registry
Production:       ECS Fargate / Cloud Run / EKS
```

```yaml
# Example: AWS ECS Task Definition (what replaces docker-compose.yml in production)
# This is configured via Terraform or AWS CDK, not manually
{
  "family": "payment-service",
  "containerDefinitions": [{
    "name": "payment-service",
    "image": "your-account.dkr.ecr.ap-south-1.amazonaws.com/payment-service:v2.4.1",
    "memory": 512,
    "cpu": 256,
    "essential": true,
    "environment": [],  # No plaintext secrets here
    "secrets": [
      {
        "name": "SPRING_DATASOURCE_PASSWORD",
        "valueFrom": "arn:aws:secretsmanager:ap-south-1:123:secret:prod/payment/db-password"
      }
    ],
    "healthCheck": {
      "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
      "interval": 30,
      "timeout": 10,
      "retries": 3,
      "startPeriod": 60
    }
  }]
}
```

---

## Key Takeaways

1. **Multi-stage Dockerfiles** are mandatory — never ship JDK in production images
2. **Layer order is a performance optimization** — stable layers first, volatile last
3. **`UseContainerSupport`** is critical — without it, JVM over-allocates memory in Kubernetes
4. **`depends_on: condition: service_healthy`** — not just `depends_on: service-name`
5. **Non-root user** in every production image — security scanners enforce this
6. **Never bake secrets into images** — inject at runtime via environment or Docker secrets
7. **`.dockerignore`** before any `COPY . .` — or your image has your entire git history
8. **Docker Compose is for development** — graduate to ECS/Cloud Run/Kubernetes for production
9. **Service name DNS resolution** — inside a network, use service names, not localhost
10. **`docker compose down -v` destroys data** — know the difference before running it in panic

> [!NOTE]
> Uber's container infrastructure manages over 4,000 microservices, each with its own Docker image. Their build system generates optimized Docker images in under 60 seconds by using remote layer caches and pre-built base images with all common dependencies. The same principles you apply here — layer caching, minimal base images, secrets injection — scale to that level.
## Book-Aligned Correction

The book highlights Spring Boot's Docker Compose integration:

- Spring Boot can detect `compose.yaml`, `compose.yml`, `docker-compose.yaml`, or `docker-compose.yml`.
- With `spring-boot-docker-compose`, local services like PostgreSQL/RabbitMQ can be started for development.
- Spring Boot can derive connection details from Compose service metadata in local dev.

This is not a replacement for production deployment config. It is a development accelerator.

Correct separation:

```text
local:
  docker-compose + spring-boot-docker-compose

production:
  explicit env vars + secrets + platform service discovery + health checks
```
