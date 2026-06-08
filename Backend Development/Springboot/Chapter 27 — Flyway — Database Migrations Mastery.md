# Backend Engineering with Spring Boot & Kotlin

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

## The DeliveryApp Builder's Guide

---

# Chapter 27 — Flyway: Database Migrations Mastery

### _Version-control your database exactly the way you version-control your code_

---

## 27.1 What Is Flyway and Why It Is Non-Negotiable

Your Spring Boot application has two moving parts that change constantly:

1. **Your Kotlin code** — tracked by Git. Every change is committed, versioned, reviewable, reversible.
2. **Your PostgreSQL schema** — tables, columns, indexes, constraints, foreign keys, check constraints.

Git handles code perfectly. But your database schema? Without a migration tool it is completely unmanaged chaos.

Here is what happens on a real project without Flyway after 3 months:

```
Your laptop:          V1 schema + 11 manual ALTER TABLEs you ran in psql
Teammate's laptop:    V1 schema + 6 different ALTER TABLEs
Staging server:       V1 schema + whatever the DevOps guy did 2 weeks ago
Production:           V1 schema + nobody knows what is in there
```

Nobody can answer: _"What is the exact state of production right now?"_
When you deploy: columns your Kotlin code references don't exist. Constraints that prevent bad data are missing. You `ALTER TABLE` in production in a panic while your app is throwing 500s.

**Flyway makes this impossible.** It is a database migration tool built into Spring Boot that:

- Tracks every schema change as a **versioned SQL file** (`V1__...sql`, `V2__...sql`)
- Runs each file **exactly once**, in order, on every environment
- Stores what has run in a `flyway_schema_history` table it owns
- **Refuses to start your application** if the stored history does not match the files on disk
- Makes every environment — dev, staging, production — always **identical**

The mental model is exact: **treat SQL migration files like Git commits**. Once committed (run), they are permanent. You never go back and edit history. You only add new commits (new migration files).

---

## 27.1.1 Pros and Cons of Flyway — The Full Picture

Before committing to Flyway, you need to understand what you gain and what you give up. Every tool has trade-offs.

### ✅ Pros

| Benefit | Why It Matters |
|---|---|
| **Version-controlled schema** | Every schema change is a file in Git. You see who changed what, when, and why — exactly like code reviews. |
| **Identical environments** | Dev, staging, and production always have the exact same schema. No more "works on my machine" schema bugs. |
| **Automatic execution on startup** | Flyway runs pending migrations when your Spring Boot app starts. No manual steps, no forgotten SQL. |
| **Checksum protection** | If anyone edits a past migration file, Flyway detects the mismatch and refuses to start. History is tamper-proof. |
| **Simple mental model** | Plain SQL files, numbered in order. No DSL, no XML, no abstraction layer. Developers already know SQL. |
| **Full control** | You write the exact SQL. Nothing is auto-generated or guessed. Every index, constraint, and column type is deliberate. |
| **Spring Boot autoconfiguration** | Zero boilerplate. Add the dependency, put files in `db/migration/`, done. Spring Boot wires everything automatically. |
| **Works with any CI/CD pipeline** | Migrations run on app startup. Your deploy pipeline does not need a separate migration step. |
| **Rollback-able (manually)** | You write undo migrations yourself. This forces you to think about rollback explicitly, which is actually safer. |
| **Free for the core feature set** | The community edition covers everything most apps need: versioned migrations, repeatable migrations, callbacks. |

### ❌ Cons

| Drawback | Why It Hurts | Mitigation |
|---|---|---|
| **No automatic rollback** | Flyway Community does not roll back a failed migration automatically. You write undo migrations yourself. | Plan your migrations to be forward-only and non-destructive. Use expand-contract pattern. |
| **Pure SQL only** | You cannot use ORM abstractions. You must write raw SQL. If you switch databases (PostgreSQL → MySQL), SQL files may not be compatible. | Use ANSI SQL where possible for portability. Keep PostgreSQL-specific SQL in clearly marked files. |
| **Migration file is permanent** | Once a file runs anywhere, you cannot change it. Fixing a mistake requires a new file. | Careful review before merging. Use IntelliJ's database tools to validate SQL before committing. |
| **Version conflicts in teams** | Two developers creating V5 on different branches causes a conflict that must be resolved manually. | Team convention: claim the next version number in a shared document or issue tracker. |
| **Startup time** | Every app startup validates all checksums and runs pending migrations. On large migration sets this adds seconds to startup. | Acceptable trade-off. Optimize by keeping individual migrations fast (avoid full-table scans in migrations). |
| **Large data migrations are slow** | Migrating millions of rows inside a migration file locks the table and delays startup by minutes. | Batch large data migrations or run them as background jobs outside Flyway. |
| **IntelliJ can generate scripts, but you must review them** | Auto-generated DDL from IntelliJ or Hibernate may not match your intentions exactly. | Always review generated SQL before using it as a migration. Never blindly copy auto-generated code. |

### Flyway vs Hibernate ddl-auto — Side by Side

This is the core decision every Spring Boot developer faces. Here is exactly what each option does and why you should never use `update` in production:

```yaml
# Option 1 — ddl-auto: none
# Hibernate does NOTHING to the schema. Flyway is 100% in charge.
# Best for: production environments, serious projects
spring.jpa.hibernate.ddl-auto: none
```

```yaml
# Option 2 — ddl-auto: validate  ← RECOMMENDED with Flyway
# Hibernate reads your @Entity classes and CHECKS that every field has a matching
# column in the database. If a column is missing → app refuses to start.
# It does NOT create or alter anything. Just validates.
# Best for: catching the mistake where you added a field but forgot the migration.
spring.jpa.hibernate.ddl-auto: validate
```

```yaml
# Option 3 — ddl-auto: update  ← DANGEROUS, NEVER USE IN PRODUCTION
# Hibernate reads your @Entity classes and tries to make the database match.
# What it does:
#   ✅ Adds new columns when you add a field to an entity
#   ✅ Creates new tables when you add a new entity
# What it DOES NOT do (and this is the killer):
#   ❌ Never drops a column even if you delete the field from the entity
#   ❌ Cannot rename a column — it creates a NEW column and leaves the old one
#   ❌ Cannot change column types safely
#   ❌ Leaves no history — you cannot see what changed or when
#   ❌ No checksum protection — anyone can silently make changes
# Result: Your DB fills with ghost columns over time, renames silently fail,
#         and nobody knows the actual current schema.
spring.jpa.hibernate.ddl-auto: update
```

```yaml
# Option 4 — ddl-auto: create  ← DESTROYS ALL DATA ON STARTUP
# Drops ALL tables and recreates them from entity classes every time the app starts.
# Useful ONLY for throw-away experiments.
# Never use if the data matters.
spring.jpa.hibernate.ddl-auto: create
```

```yaml
# Option 5 — ddl-auto: create-drop  ← DESTROYS DATA ON SHUTDOWN TOO
# Creates tables on startup. Drops ALL tables on shutdown.
# Used by @DataJpaTest automatically for isolated slice tests.
# Never in production.
spring.jpa.hibernate.ddl-auto: create-drop
```

### The Real Danger of ddl-auto: update — A Concrete Example

You have this entity:
```kotlin
@Entity
class Product(
    val id: Long? = null,
    val productName: String  // field named productName → column "product_name"
)
```

You decide to rename `productName` to `name` in the entity:
```kotlin
@Entity
class Product(
    val id: Long? = null,
    val name: String  // renamed from productName → column "name"
)
```

**With `ddl-auto: update`:**
```sql
-- What Hibernate does (silently, with no log):
ALTER TABLE products ADD COLUMN name VARCHAR(255);
-- It does NOT drop product_name.
-- It does NOT copy data from product_name to name.
-- All existing data is now in product_name but your code reads from name.
-- All existing products now have NULL name. Your app breaks silently.
```

**With Flyway:**
```sql
-- V7__rename_product_name_to_name.sql
-- You write this intentionally. You know exactly what happens.
ALTER TABLE products RENAME COLUMN product_name TO name;
-- Data intact. Column renamed. History recorded. Checksum stored.
```

Flyway forces you to be explicit. `ddl-auto: update` hides the problem and creates a worse one.

---

## 27.2 How Flyway Works Internally — Deep Dive

Understanding the internals is what separates developers who use Flyway from those who understand it.

### The flyway_schema_history Table

On first run, Flyway creates this table in your database:

```sql
-- Flyway creates this automatically. You never create it manually.
CREATE TABLE flyway_schema_history (
    installed_rank  INTEGER      NOT NULL,         -- execution order
    version         VARCHAR(50),                   -- "1", "2", "3" etc.
    description     VARCHAR(200) NOT NULL,         -- from your filename
    type            VARCHAR(20)  NOT NULL,         -- SQL, JDBC, SPRING_JDBC etc.
    script          VARCHAR(1000) NOT NULL,        -- the filename
    checksum        INTEGER,                       -- CRC32 of file content
    installed_by    VARCHAR(100) NOT NULL,         -- database user who ran it
    installed_on    TIMESTAMP    NOT NULL DEFAULT NOW(),
    execution_time  INTEGER      NOT NULL,         -- milliseconds it took
    success         BOOLEAN      NOT NULL,         -- true if it completed
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);
```

After running three migrations, it contains:

| installed_rank | version | description           | script                        | checksum    | success |
|---|---|---|---|---|---|
| 1 | 1 | create initial schema | V1__create_initial_schema.sql | -1453219832 | true |
| 2 | 2 | create indexes        | V2__create_indexes.sql        | 892034512   | true |
| 3 | 3 | add phone to users    | V3__add_phone_to_users.sql    | 234892341   | true |

### The Exact Startup Sequence

Every time your Spring Boot app starts, Flyway runs this sequence **before** your application context finishes loading:

```
Step 1 → Flyway connects to the database (same datasource as your app)
Step 2 → Creates flyway_schema_history if it does not exist yet
Step 3 → Scans db/migration/ for all .sql files matching the naming pattern
Step 4 → Sorts those files by version number
Step 5 → For each file already in flyway_schema_history:
           → Recompute CRC32 checksum of the file on disk
           → Compare with stored checksum
           → MISMATCH → throw FlywayException, app REFUSES TO START
           → MATCH    → skip (already applied)
Step 6 → For each file NOT in flyway_schema_history:
           → Run the SQL
           → On success: insert a row in flyway_schema_history with success=true
           → On failure: insert a row with success=false, throw exception, app STOPS
Step 7 → All pending migrations applied → Spring context finishes loading → app starts
```

This happens in order, synchronously, before your controllers are ready. No request ever hits your API on an out-of-date schema.

### The Checksum — Why It Protects You

The checksum is a CRC32 hash computed from the file content. Flyway stores it when the migration runs. On every subsequent startup it recomputes the hash and compares.

If you change even a single character in `V1__create_initial_schema.sql` after it has run, the checksum changes. Flyway detects the mismatch and **refuses to start**:

```
org.flywaydb.core.api.exception.FlywayValidateException:
Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 1
-> Applied to database : 892034512
-> Resolved locally    : -1998012345
```

This is intentional. It protects you from someone editing a migration that already ran on production, which would make your history table a lie.

---

## 27.3 Complete Project Setup — build.gradle.kts + application.yml

### build.gradle.kts — Full Dependencies

```kotlin
// build.gradle.kts

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"

    // Optional: run migrations via Gradle task (./gradlew flywayMigrate)
    id("org.flywaydb.flyway") version "10.15.0"
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Flyway — core + PostgreSQL dialect (required from Flyway 10+)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // PostgreSQL JDBC driver
    runtimeOnly("org.postgresql:postgresql")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}

// Optional: Configure Flyway Gradle plugin for CLI-style commands
flyway {
    url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/deliveryapp"
    user = System.getenv("DB_USER") ?: "postgres"
    password = System.getenv("DB_PASSWORD") ?: "yourpassword"
    locations = arrayOf("classpath:db/migration")
    cleanDisabled = true  // NEVER allow flyway clean in production
}
```

### application.yml — Every Setting Explained

```yaml
# src/main/resources/application.yml

spring:

  datasource:
    url: jdbc:postgresql://localhost:5432/deliveryapp
    username: postgres
    password: yourpassword
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      # CRITICAL CHOICE — pick one:
      #
      # validate → Hibernate checks that entity fields match database columns.
      #            If an entity has a field with no matching column, app REFUSES to start.
      #            This is a safety net. Use this. It catches forgotten migrations.
      #
      # none     → Hibernate does absolutely nothing to the schema.
      #            Use this if you don't want Hibernate validating at all.
      #
      # NEVER use: create, create-drop, update — these let Hibernate mutate your schema
      # which conflicts with Flyway and destroys data.
      ddl-auto: validate

    open-in-view: false           # Never keep DB session open for full HTTP request
    show-sql: false               # Set true in dev to see generated SQL
    properties:
      hibernate:
        format_sql: true          # Pretty-print SQL when show-sql is true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    # Turn Flyway on/off entirely (never turn off in production)
    enabled: true

    # Where Flyway scans for migration files.
    # classpath: reads from src/main/resources/
    # filesystem: reads from an absolute path on disk
    locations:
      - classpath:db/migration

    # The table Flyway uses to track what has run.
    # Default is flyway_schema_history. Only change if you have a naming convention.
    table: flyway_schema_history

    # PostgreSQL schema to apply migrations to.
    # If not set, uses the default schema of the datasource user.
    schemas:
      - public

    # Recompute and verify checksums of already-applied migrations on every startup.
    # Always keep true. Protects against silent history corruption.
    validate-on-migrate: true

    # Allow Flyway to run V3 even if V4 has already run (out of order).
    # Keep FALSE. If set true, Flyway allows dangerous out-of-order migrations.
    out-of-order: false

    # When adding Flyway to an existing database that already has tables.
    # See section 27.10 for full explanation.
    baseline-on-migrate: false
    baseline-version: 0

    # Completely disable the flyway:clean command.
    # clean drops ALL tables. NEVER allow it in production.
    clean-disabled: true

    # Encoding of your .sql files
    encoding: UTF-8

    # Optional: substitute ${placeholder} tokens inside your SQL files
    placeholders:
      app_schema: public
      environment: local

    # Connection — Flyway inherits from spring.datasource by default.
    # Only set explicitly if Flyway needs a different user (e.g., a superuser for DDL).
    # url: ${spring.datasource.url}
    # user: ${spring.datasource.username}
    # password: ${spring.datasource.password}
```

### Directory Structure — Where Files Must Live

```
src/
└── main/
    ├── kotlin/
    │   └── com/example/deliveryapp/
    │       ├── DeliveryAppApplication.kt
    │       ├── entity/
    │       │   ├── User.kt
    │       │   ├── Product.kt
    │       │   ├── Order.kt
    │       │   └── Enums.kt
    │       ├── repository/
    │       ├── service/
    │       └── controller/
    └── resources/
        ├── application.yml
        └── db/
            └── migration/
                ├── V1__create_initial_schema.sql    ← runs first, once, never again
                ├── V2__create_indexes.sql           ← runs after V1, once, never again
                ├── V3__add_phone_to_users.sql       ← runs after V2, once, never again
                └── R__create_reporting_views.sql    ← repeatable, re-runs when changed
```

The path `db/migration` matches `classpath:db/migration` exactly. Every file in that folder is scanned.

---

## 27.3.1 IntelliJ IDEA Tooling — Generate and Validate Migration SQL

IntelliJ IDEA (Ultimate edition) has built-in database tools that can help you write migration SQL. The YouTube tutorial mentions this — but also warns: **always review and manually control your SQL**. Never blindly use auto-generated DDL.

### How to Connect IntelliJ to Your Local PostgreSQL

```
1. Open IntelliJ → View → Tool Windows → Database
2. Click "+" → Data Source → PostgreSQL
3. Fill in:
   Host:     localhost
   Port:     5432
   Database: deliveryapp
   User:     postgres
   Password: yourpassword
4. Click "Test Connection" → should say "Successful"
5. Click OK
```

Now IntelliJ can see your actual tables, columns, indexes, and constraints.

### Using IntelliJ to Generate Migration SQL From Entity Changes

When you add a new field to a Kotlin entity, IntelliJ can show you what SQL would need to run:

```
1. Open the Database panel → right-click your schema → "Modify Schema"
   OR
1. Open your @Entity class
2. Right-click inside the class → "Show in Database" (if the plugin is active)
3. IntelliJ compares the entity definition to the actual table
4. It shows a diff: "Column 'loyalty_points' is missing from table 'users'"
5. It can generate: ALTER TABLE users ADD COLUMN loyalty_points INTEGER NOT NULL DEFAULT 0;
```

**⚠️ Do not use it blindly.** IntelliJ generates SQL based on what it thinks you want. It may:
- Use a different data type than you intended (e.g., `BIGINT` when you wanted `INTEGER`)
- Miss constraints (`NOT NULL`, `UNIQUE`, `CHECK`)
- Use a platform-specific syntax that does not match your PostgreSQL version
- Not add the index you need for the foreign key

**The correct workflow:**
```
1. Make the change in your @Entity class
2. Use IntelliJ to generate a DRAFT of the SQL
3. Copy that draft into a new Vn__.sql migration file
4. READ IT. Edit it. Add missing constraints, indexes, defaults.
5. Test it in your local PostgreSQL
6. Commit the final, reviewed SQL file
```

### IntelliJ's SQL Editor for Migration Files

When you open a `.sql` file inside `db/migration/`, IntelliJ:
- Highlights syntax errors
- Autocompletes table names and column names (if DB is connected)
- Underlines SQL that would fail (e.g., referencing a table that doesn't exist)
- Shows the execution plan for `SELECT` statements

Enable SQL dialect for your files:
```
File → Settings → Languages & Frameworks → SQL Dialects
Project SQL Dialect: PostgreSQL
```

### Viewing flyway_schema_history in IntelliJ

After your app runs, open the Database panel:
```
 deliveryapp
 └── public
     └── Tables
         ├── flyway_schema_history  ← double-click to see all migrations that ran
         ├── users
         ├── products
         └── orders
```

Double-clicking `flyway_schema_history` shows you the full migration history table as a grid — version, description, checksum, installed_on, success. This is the fastest way to check the current migration state of your local database without running any SQL.

---

## 27.4 Migration File Naming — The Rules That Cause Silent Failures

This is the number one source of confusion for Flyway beginners. Flyway does **not error** on a wrongly named file. It silently ignores it. Your migration never runs and you spend an hour wondering why.

### The Full Pattern

```
{prefix}{separator}{description}{suffix}

prefix      →  V  (versioned)  |  R  (repeatable)  |  U  (undo, Teams only)
separator   →  __ (two underscores — the most commonly missed thing)
description →  any text with underscores or letters (no spaces)
suffix      →  .sql  (lowercase on case-sensitive Linux filesystems)
```

### Versioned Migrations — V prefix

```
V1__create_initial_schema.sql
V2__create_performance_indexes.sql
V3__add_phone_column_to_users.sql
V4__create_coupons_table.sql
V5__add_coupon_id_to_orders.sql
V10__add_full_text_search_columns.sql
V11__backfill_user_slugs.sql
V100__add_delivery_partner_tables.sql
```

Version numbers are compared **numerically**: V2 < V10 < V11 < V100. Unlike string comparison, V10 is greater than V9, not V1.

You can also use decimals for branching:
```
V1.0__create_schema.sql
V1.1__add_missing_column.sql   ← inserted between V1 and V2 without renumbering
V2.0__create_orders.sql
```

### What Flyway Silently Ignores — Memorize These

```
V1_create_schema.sql           → ONE underscore — ignored
v1__create_schema.sql          → lowercase v — ignored
V1__create schema.sql          → space in description — ignored
V1__create_schema.SQL          → uppercase .SQL on Linux — ignored
create_schema.sql              → no prefix at all — ignored
V1.sql                         → no description — ignored
_V1__create_schema.sql         → leading underscore — ignored
```

> **How to debug a silent ignore:** Add `logging.level.org.flywaydb=DEBUG` to your `application.yml` temporarily. Flyway will log every file it finds, every file it skips, and why. This immediately reveals naming problems.

### Logging to Diagnose Silent Ignores

```yaml
# application.yml — add temporarily to debug missing migrations

logging:
  level:
    org.flywaydb: DEBUG
```

Flyway will now log something like:
```
DEBUG Scanning for resources at: classpath:db/migration
DEBUG Found resource: V1__create_initial_schema.sql
DEBUG Skipping: V2_wrong_name.sql (does not match Flyway pattern)
DEBUG Found resource: V3__add_phone_to_users.sql
```

---

## 27.5 Writing Migration SQL — Every Pattern With Full Examples

### V1 — The Initial Schema (Complete DeliveryApp)

Your first migration captures the entire starting state of your database. Write it once. Never touch it again.

```sql
-- src/main/resources/db/migration/V1__create_initial_schema.sql
--
-- This is the foundation of the entire DeliveryApp schema.
-- This file runs ONCE on every environment and is permanently frozen.
-- Any changes to the schema go in V2, V3, V4... never here.

-- ── USERS ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    full_name       VARCHAR(255)  NOT NULL,
    phone           VARCHAR(20),
    avatar_url      TEXT,
    is_premium      BOOLEAN       NOT NULL DEFAULT FALSE,
    loyalty_points  INTEGER       NOT NULL DEFAULT 0,
    referral_code   VARCHAR(20)   NOT NULL UNIQUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── CATEGORIES ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    image_url   TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── PRODUCTS ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    slug            VARCHAR(255)    NOT NULL UNIQUE,
    price           DECIMAL(10, 2)  NOT NULL,
    mrp             DECIMAL(10, 2),              -- maximum retail price
    stock_quantity  INTEGER         NOT NULL DEFAULT 0,
    category_id     BIGINT          NOT NULL REFERENCES categories(id),
    image_url       TEXT,
    weight_grams    INTEGER,
    brand           VARCHAR(100),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_products_price_positive CHECK (price > 0),
    CONSTRAINT chk_products_stock_non_negative CHECK (stock_quantity >= 0)
);

-- ── ORDERS ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL      PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users(id),
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    total_amount     DECIMAL(10, 2) NOT NULL,
    discount_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    final_amount     DECIMAL(10, 2) NOT NULL,
    delivery_address TEXT           NOT NULL,
    delivery_lat     DECIMAL(9, 6),
    delivery_lng     DECIMAL(9, 6),
    notes            TEXT,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    confirmed_at     TIMESTAMP,
    delivered_at     TIMESTAMP,
    cancelled_at     TIMESTAMP,
    CONSTRAINT chk_orders_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED')
    ),
    CONSTRAINT chk_orders_final_amount_positive CHECK (final_amount >= 0)
);

-- ── ORDER ITEMS ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id          BIGSERIAL      PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  BIGINT         NOT NULL REFERENCES products(id),
    quantity    INTEGER        NOT NULL,
    unit_price  DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,   -- quantity * unit_price, stored for history
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price_positive CHECK (unit_price > 0)
);

-- ── ADDRESSES ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS addresses (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label        VARCHAR(50)  NOT NULL DEFAULT 'HOME',  -- HOME, WORK, OTHER
    full_address TEXT         NOT NULL,
    landmark     VARCHAR(255),
    city         VARCHAR(100) NOT NULL,
    pincode      VARCHAR(10)  NOT NULL,
    lat          DECIMAL(9, 6),
    lng          DECIMAL(9, 6),
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### V2 — Performance Indexes

Always separate your indexes into a dedicated migration. Indexes can be expensive and slow to create on large tables.

```sql
-- src/main/resources/db/migration/V2__create_performance_indexes.sql
--
-- All performance indexes for the V1 schema.
-- Separated from V1 so we can add/remove indexes independently.

-- USERS — lookup by email (login), referral code
CREATE INDEX IF NOT EXISTS idx_users_email        ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_referral_code ON users(referral_code);
CREATE INDEX IF NOT EXISTS idx_users_created_at   ON users(created_at DESC);

-- PRODUCTS — filter by category, sort by price, filter active
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_is_active   ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_price       ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_slug        ON products(slug);
CREATE INDEX IF NOT EXISTS idx_products_created_at  ON products(created_at DESC);

-- ORDERS — always queried by user, always filtered by status
CREATE INDEX IF NOT EXISTS idx_orders_user_id    ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);

-- ORDER ITEMS — always fetched by order
CREATE INDEX IF NOT EXISTS idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- ADDRESSES — always fetched by user
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses(user_id);
```

### V3 — Adding a Column to an Existing Table

The most common migration you will write throughout the lifetime of a project.

```sql
-- src/main/resources/db/migration/V3__add_coupon_support_to_orders.sql
--
-- Add coupon tracking to orders.
-- Step 1 only: adding the column. FK constraint comes in V4 after coupons table exists.
-- This separation is intentional: if something goes wrong, the rollback is minimal.

-- ADD COLUMN IF NOT EXISTS — safe to run twice (idempotent)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50);

-- Add column with a NOT NULL default — existing rows get the default automatically
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS coupon_discount DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
```

### V4 — Creating a New Table With a Foreign Key Back to an Existing Table

```sql
-- src/main/resources/db/migration/V4__create_coupons_table.sql

CREATE TABLE IF NOT EXISTS coupons (
    id              BIGSERIAL      PRIMARY KEY,
    code            VARCHAR(50)    NOT NULL UNIQUE,
    description     VARCHAR(255),
    discount_type   VARCHAR(20)    NOT NULL DEFAULT 'PERCENTAGE',  -- PERCENTAGE or FLAT
    discount_value  DECIMAL(10, 2) NOT NULL,
    min_order_value DECIMAL(10, 2) NOT NULL DEFAULT 0,
    max_discount    DECIMAL(10, 2),                                -- cap for PERCENTAGE type
    max_uses        INTEGER        NOT NULL DEFAULT 100,
    used_count      INTEGER        NOT NULL DEFAULT 0,
    is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_coupons_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FLAT')),
    CONSTRAINT chk_coupons_discount_positive CHECK (discount_value > 0)
);

-- Track which user used which coupon on which order
CREATE TABLE IF NOT EXISTS coupon_uses (
    id         BIGSERIAL PRIMARY KEY,
    coupon_id  BIGINT    NOT NULL REFERENCES coupons(id),
    user_id    BIGINT    NOT NULL REFERENCES users(id),
    order_id   BIGINT    NOT NULL REFERENCES orders(id),
    used_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (coupon_id, user_id)  -- each user can use a coupon only once
);

-- Index coupon code for fast lookup on checkout
CREATE INDEX IF NOT EXISTS idx_coupons_code      ON coupons(code);
CREATE INDEX IF NOT EXISTS idx_coupons_is_active ON coupons(is_active);
CREATE INDEX IF NOT EXISTS idx_coupon_uses_user  ON coupon_uses(user_id);
```

### V5 — Adding a Foreign Key to an Existing Column

After creating the `coupons` table in V4, we can now add the FK from the `orders.coupon_code` column.

```sql
-- src/main/resources/db/migration/V5__link_orders_to_coupons.sql
--
-- Now that the coupons table exists (V4), we can link orders to it via FK.
-- We use coupon_code (not coupon_id) so historical orders keep the code
-- even if the coupon is deleted.

-- The coupon_code column was added in V3 and is already nullable.
-- No schema change needed — just add an index for join performance.
-- FK on a text column would break if coupon is ever deleted, so we skip FK
-- and rely on application-level integrity. This is intentional.

CREATE INDEX IF NOT EXISTS idx_orders_coupon_code ON orders(coupon_code)
    WHERE coupon_code IS NOT NULL;  -- partial index: only indexes rows that have a coupon
```

### V6 — Renaming a Column (Zero-Downtime Pattern)

Renaming a column while keeping the app running requires a 3-step migration across 3 deployments. Here is the pattern:

**Step 1 (V6): Add new column, keep old column**

```sql
-- src/main/resources/db/migration/V6__rename_full_name_step1_add_new.sql
--
-- ZERO-DOWNTIME RENAME PATTERN — Step 1 of 3
-- We are renaming users.full_name → users.display_name
--
-- Step 1: Add the new column. Both columns exist simultaneously.
--         Application code in next deploy will write to BOTH columns.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);

-- Copy existing data into the new column
UPDATE users
SET display_name = full_name
WHERE display_name IS NULL;
```

**Step 2 (V7): Drop old column** ← deploy after new code ships

```sql
-- src/main/resources/db/migration/V7__rename_full_name_step2_drop_old.sql
--
-- ZERO-DOWNTIME RENAME PATTERN — Step 2
-- Old code is no longer deployed. Safe to drop the old column.

ALTER TABLE users
    DROP COLUMN IF EXISTS full_name;

-- Make the new column NOT NULL now that all rows have been backfilled
ALTER TABLE users
    ALTER COLUMN display_name SET NOT NULL;
```

### V8 — Data Migration (Backfill Existing Rows)

Data migrations update existing rows. They are the trickiest because they must be:
- **Idempotent** — safe to run again if they fail halfway
- **Batched** — large tables cannot be updated in one transaction (locks the table)

```sql
-- src/main/resources/db/migration/V8__backfill_product_slugs.sql
--
-- Products added before slug was introduced have NULL slugs.
-- Generate slugs from the product name for all of them.
-- Idempotent: WHERE slug IS NULL means re-running is safe.

UPDATE products
SET slug = lower(
    regexp_replace(
        regexp_replace(name, '[^a-zA-Z0-9\s]', '', 'g'),  -- remove non-alphanumeric
        '\s+', '-', 'g'                                    -- replace spaces with hyphens
    )
) || '-' || id   -- append id to guarantee uniqueness (e.g., "apple-juice-42")
WHERE slug IS NULL OR slug = '';

-- Now enforce NOT NULL since all rows are backfilled
-- (only do this AFTER confirming the UPDATE completed successfully)
ALTER TABLE products
    ALTER COLUMN slug SET NOT NULL;
```

### V9 — Adding a Full-Text Search Column

A complex migration for search functionality. This is what production PostgreSQL FTS setup looks like.

```sql
-- src/main/resources/db/migration/V9__add_full_text_search.sql
--
-- Add PostgreSQL tsvector column to products for full-text search.
-- This enables fast searches like "organic apple juice" across name + description.

-- Step 1: Add the tsvector column
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS search_vector TSVECTOR;

-- Step 2: Backfill all existing products
UPDATE products
SET search_vector = to_tsvector(
    'english',
    coalesce(name, '') || ' ' ||
    coalesce(description, '') || ' ' ||
    coalesce(brand, '')
);

-- Step 3: Create a GIN index on the tsvector column (required for fast FTS)
CREATE INDEX IF NOT EXISTS idx_products_search_vector
    ON products USING GIN (search_vector);

-- Step 4: Create a trigger to automatically update search_vector on INSERT/UPDATE
-- This ensures the search index stays current without application-level code.
CREATE OR REPLACE FUNCTION update_product_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector(
        'english',
        coalesce(NEW.name, '') || ' ' ||
        coalesce(NEW.description, '') || ' ' ||
        coalesce(NEW.brand, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_products_search_vector ON products;
CREATE TRIGGER trg_products_search_vector
    BEFORE INSERT OR UPDATE OF name, description, brand
    ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_product_search_vector();
```

### V10 — Delivery and Driver Tables (Multi-Table Migration)

```sql
-- src/main/resources/db/migration/V10__create_delivery_tracking.sql
--
-- Full delivery tracking system: drivers, deliveries, real-time location.

CREATE TABLE IF NOT EXISTS drivers (
    id              BIGSERIAL      PRIMARY KEY,
    user_id         BIGINT         NOT NULL REFERENCES users(id) UNIQUE,
    vehicle_type    VARCHAR(30)    NOT NULL DEFAULT 'BIKE',
    license_plate   VARCHAR(20)    NOT NULL UNIQUE,
    is_available    BOOLEAN        NOT NULL DEFAULT FALSE,
    is_verified     BOOLEAN        NOT NULL DEFAULT FALSE,
    current_lat     DECIMAL(9, 6),
    current_lng     DECIMAL(9, 6),
    last_location_at TIMESTAMP,
    rating          DECIMAL(3, 2)  NOT NULL DEFAULT 5.00,
    total_deliveries INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_drivers_vehicle_type CHECK (vehicle_type IN ('BIKE', 'SCOOTER', 'CAR')),
    CONSTRAINT chk_drivers_rating CHECK (rating BETWEEN 0 AND 5)
);

CREATE TABLE IF NOT EXISTS deliveries (
    id                  BIGSERIAL   PRIMARY KEY,
    order_id            BIGINT      NOT NULL REFERENCES orders(id) UNIQUE,
    driver_id           BIGINT      REFERENCES drivers(id),
    status              VARCHAR(30) NOT NULL DEFAULT 'WAITING_FOR_DRIVER',
    pickup_lat          DECIMAL(9, 6) NOT NULL,
    pickup_lng          DECIMAL(9, 6) NOT NULL,
    dropoff_lat         DECIMAL(9, 6) NOT NULL,
    dropoff_lng         DECIMAL(9, 6) NOT NULL,
    distance_km         DECIMAL(6, 2),
    estimated_minutes   INTEGER,
    actual_minutes      INTEGER,
    driver_assigned_at  TIMESTAMP,
    picked_up_at        TIMESTAMP,
    delivered_at        TIMESTAMP,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_deliveries_status CHECK (
        status IN ('WAITING_FOR_DRIVER', 'DRIVER_ASSIGNED', 'PICKED_UP', 'DELIVERED', 'FAILED')
    )
);

-- Location history — every GPS ping from driver stored here
CREATE TABLE IF NOT EXISTS driver_locations (
    id         BIGSERIAL      PRIMARY KEY,
    driver_id  BIGINT         NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    lat        DECIMAL(9, 6)  NOT NULL,
    lng        DECIMAL(9, 6)  NOT NULL,
    recorded_at TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_drivers_is_available      ON drivers(is_available);
CREATE INDEX IF NOT EXISTS idx_drivers_current_location  ON drivers(current_lat, current_lng)
    WHERE is_available = TRUE;   -- partial index: only index available drivers
CREATE INDEX IF NOT EXISTS idx_deliveries_driver_id      ON deliveries(driver_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_status         ON deliveries(status);
CREATE INDEX IF NOT EXISTS idx_driver_locations_driver   ON driver_locations(driver_id, recorded_at DESC);
```

---

## 27.6 The Kotlin Entity Side — Keeping JPA in Sync With Migrations

Every migration you write must be reflected in your Kotlin entity classes. Here is the complete entity set that maps to the schema above.

### Enums.kt

```kotlin
// src/main/kotlin/com/example/deliveryapp/entity/Enums.kt

package com.example.deliveryapp.entity

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

enum class CouponDiscountType {
    PERCENTAGE,
    FLAT
}

enum class VehicleType {
    BIKE,
    SCOOTER,
    CAR
}

enum class DeliveryStatus {
    WAITING_FOR_DRIVER,
    DRIVER_ASSIGNED,
    PICKED_UP,
    DELIVERED,
    FAILED
}

enum class AddressLabel {
    HOME,
    WORK,
    OTHER
}
```

### User.kt

```kotlin
// src/main/kotlin/com/example/deliveryapp/entity/User.kt

package com.example.deliveryapp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_users_email", columnNames = ["email"]),
        UniqueConstraint(name = "uq_users_referral_code", columnNames = ["referral_code"])
    ]
)
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Maps to column "email" — Spring auto-converts camelCase to snake_case
    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    // This was renamed from "name" to "display_name" in V6/V7 migrations
    // The field name in Kotlin (displayName) auto-maps to column "display_name"
    @Column(name = "display_name", nullable = false, length = 255)
    var displayName: String,

    @Column(length = 20)
    var phone: String? = null,

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    var avatarUrl: String? = null,

    @Column(name = "is_premium", nullable = false)
    var isPremium: Boolean = false,

    @Column(name = "loyalty_points", nullable = false)
    var loyaltyPoints: Int = 0,

    // Unique code used for referral program — generated by service before save
    @Column(name = "referral_code", nullable = false, unique = true, length = 20)
    val referralCode: String,

    // updatable = false → Hibernate never includes this in UPDATE statements
    // The database DEFAULT NOW() sets it on INSERT
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### Product.kt

```kotlin
// src/main/kotlin/com/example/deliveryapp/entity/Product.kt

package com.example.deliveryapp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "products",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_products_slug", columnNames = ["slug"])
    ]
)
class Product(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, unique = true, length = 255)
    var slug: String,

    // DECIMAL(10, 2) in SQL → BigDecimal in Kotlin for exact monetary math
    // Never use Double for money — floating point rounding will cause errors
    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,

    // mrp is nullable — not all products have an MRP
    @Column(precision = 10, scale = 2)
    var mrp: BigDecimal? = null,

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = 0,

    // Many products belong to ONE category
    // fetch = LAZY → only load Category when you access product.category in code
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @Column(name = "image_url", columnDefinition = "TEXT")
    var imageUrl: String? = null,

    @Column(name = "weight_grams")
    var weightGrams: Int? = null,

    @Column(length = 100)
    var brand: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()

    // NOTE: The search_vector column added in V9 is NOT mapped here.
    // It is a generated/computed column maintained by a DB trigger.
    // JPA does not manage it. You query it directly via @Query with native SQL.
)
```

### Order.kt

```kotlin
// src/main/kotlin/com/example/deliveryapp/entity/Order.kt

package com.example.deliveryapp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class Order(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Many orders belong to ONE user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    // EnumType.STRING stores "PENDING", "CONFIRMED" etc. as text
    // NEVER use EnumType.ORDINAL — adding a new enum value breaks all existing data
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    val totalAmount: BigDecimal,

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    var finalAmount: BigDecimal,

    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    val deliveryAddress: String,

    @Column(name = "delivery_lat", precision = 9, scale = 6)
    val deliveryLat: BigDecimal? = null,

    @Column(name = "delivery_lng", precision = 9, scale = 6)
    val deliveryLng: BigDecimal? = null,

    @Column(name = "coupon_code", length = 50)
    var couponCode: String? = null,

    @Column(name = "coupon_discount", precision = 10, scale = 2)
    var couponDiscount: BigDecimal = BigDecimal.ZERO,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null,

    @Column(name = "delivered_at")
    var deliveredAt: LocalDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    // ONE order has MANY order items
    // mappedBy = "order" → the OrderItem.order field owns this relationship
    // cascade = ALL → saving/deleting Order automatically saves/deletes items
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItem> = mutableListOf()
)
```

### OrderItem.kt

```kotlin
// src/main/kotlin/com/example/deliveryapp/entity/OrderItem.kt

package com.example.deliveryapp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "order_items")
class OrderItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Many items belong to ONE order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    // Many items reference ONE product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(nullable = false)
    val quantity: Int,

    // Stored at order time — even if product price changes later, order history is correct
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    val unitPrice: BigDecimal,

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    val totalPrice: BigDecimal,    // = quantity * unitPrice, stored for query performance

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### Driver.kt

```kotlin
// src/main/kotlin/com/example/deliveryapp/entity/Driver.kt

package com.example.deliveryapp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "drivers")
class Driver(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // One-to-one: each User can be a Driver
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    var vehicleType: VehicleType = VehicleType.BIKE,

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    var licensePlate: String,

    @Column(name = "is_available", nullable = false)
    var isAvailable: Boolean = false,

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false,

    @Column(name = "current_lat", precision = 9, scale = 6)
    var currentLat: BigDecimal? = null,

    @Column(name = "current_lng", precision = 9, scale = 6)
    var currentLng: BigDecimal? = null,

    @Column(name = "last_location_at")
    var lastLocationAt: LocalDateTime? = null,

    @Column(nullable = false, precision = 3, scale = 2)
    var rating: BigDecimal = BigDecimal("5.00"),

    @Column(name = "total_deliveries", nullable = false)
    var totalDeliveries: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

---

## 27.7 Repeatable Migrations — R__ Prefix

Repeatable migrations have no version. They re-run every time their content changes. Always run **after** all versioned migrations.

```
R__create_reporting_views.sql        ← re-runs whenever this file changes
R__create_helper_functions.sql       ← re-runs whenever this file changes
```

### R__create_reporting_views.sql

```sql
-- src/main/resources/db/migration/R__create_reporting_views.sql
--
-- These views are for analytics and reporting.
-- They are recreated every time this file changes (Flyway recomputes the checksum).
-- ALWAYS use CREATE OR REPLACE — never plain CREATE.

CREATE OR REPLACE VIEW daily_order_summary AS
SELECT
    DATE(created_at)        AS order_date,
    COUNT(*)                AS total_orders,
    COUNT(*) FILTER (WHERE status = 'DELIVERED')   AS delivered_orders,
    COUNT(*) FILTER (WHERE status = 'CANCELLED')   AS cancelled_orders,
    SUM(final_amount)       AS total_revenue,
    AVG(final_amount)       AS avg_order_value,
    MIN(final_amount)       AS min_order_value,
    MAX(final_amount)       AS max_order_value
FROM orders
GROUP BY DATE(created_at)
ORDER BY order_date DESC;

-- Product performance: how many times each product was ordered and total revenue
CREATE OR REPLACE VIEW product_performance AS
SELECT
    p.id                        AS product_id,
    p.name                      AS product_name,
    p.brand,
    c.name                      AS category_name,
    COUNT(oi.id)                AS total_units_sold,
    SUM(oi.quantity)            AS total_quantity_sold,
    SUM(oi.total_price)         AS total_revenue,
    AVG(oi.unit_price)          AS avg_selling_price
FROM products p
JOIN categories c ON c.id = p.category_id
LEFT JOIN order_items oi ON oi.product_id = p.id
LEFT JOIN orders o ON o.id = oi.order_id AND o.status = 'DELIVERED'
GROUP BY p.id, p.name, p.brand, c.name
ORDER BY total_revenue DESC NULLS LAST;

-- Active drivers with their current load
CREATE OR REPLACE VIEW available_drivers AS
SELECT
    d.id            AS driver_id,
    u.display_name  AS driver_name,
    u.phone,
    d.vehicle_type,
    d.current_lat,
    d.current_lng,
    d.rating,
    d.total_deliveries
FROM drivers d
JOIN users u ON u.id = d.user_id
WHERE d.is_available = TRUE
  AND d.is_verified = TRUE
  AND d.current_lat IS NOT NULL;
```

### R__create_helper_functions.sql

```sql
-- src/main/resources/db/migration/R__create_helper_functions.sql

-- Function: calculate distance between two GPS coordinates (Haversine formula)
-- Used by the delivery assignment algorithm to find the nearest driver.
CREATE OR REPLACE FUNCTION haversine_distance(
    lat1 DECIMAL, lng1 DECIMAL,
    lat2 DECIMAL, lng2 DECIMAL
) RETURNS DECIMAL AS $$
DECLARE
    r        CONSTANT DECIMAL := 6371;  -- Earth radius in km
    dlat     DECIMAL;
    dlng     DECIMAL;
    a        DECIMAL;
    c        DECIMAL;
BEGIN
    dlat := radians(lat2 - lat1);
    dlng := radians(lng2 - lng1);
    a := sin(dlat / 2) ^ 2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlng / 2) ^ 2;
    c := 2 * asin(sqrt(a));
    RETURN r * c;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function: apply coupon discount to an order amount
CREATE OR REPLACE FUNCTION apply_coupon(
    order_amount    DECIMAL,
    discount_type   VARCHAR,
    discount_value  DECIMAL,
    max_discount    DECIMAL DEFAULT NULL
) RETURNS DECIMAL AS $$
DECLARE
    discount DECIMAL;
BEGIN
    IF discount_type = 'FLAT' THEN
        discount := LEAST(discount_value, order_amount);
    ELSE  -- PERCENTAGE
        discount := order_amount * (discount_value / 100);
        IF max_discount IS NOT NULL THEN
            discount := LEAST(discount, max_discount);
        END IF;
    END IF;
    RETURN GREATEST(order_amount - discount, 0);
END;
$$ LANGUAGE plpgsql IMMUTABLE;
```

---

## 27.8 Java-Based Migrations From Kotlin — The Powerful Technique Most Developers Miss

Flyway supports migrations written in Kotlin/Java code, not just SQL. This is essential when you need to:
- Migrate data using application logic (e.g., call an external API during migration)
- Use Spring beans (like a `PasswordEncoder`) during migration
- Batch-process millions of rows using Kotlin logic instead of pure SQL

### Spring-Aware Kotlin Migration (Using Spring Beans Inside a Migration)

```kotlin
// src/main/kotlin/com/example/deliveryapp/migration/V11__backfill_referral_codes.kt
//
// IMPORTANT: This file MUST be in db/migration or a package Flyway scans.
// The class name MUST match the Flyway naming pattern exactly:
// V{version}__description  (double underscore, spaces become underscores)
//
// Register this location in application.yml:
// spring.flyway.locations: classpath:db/migration,classpath:com/example/deliveryapp/migration

package com.example.deliveryapp.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.util.UUID

/**
 * Java-based migration: backfill referral codes for all users who don't have one.
 * This uses Kotlin logic to generate codes, not raw SQL.
 *
 * BaseJavaMigration — Flyway's base class for programmatic migrations.
 */
class V11__backfill_referral_codes : BaseJavaMigration() {

    override fun migrate(context: Context) {
        // Wrap the Flyway-provided connection in a JdbcTemplate for convenience
        val jdbcTemplate = JdbcTemplate(
            SingleConnectionDataSource(context.connection, true)
        )

        // Find all users without a referral code
        val userIds = jdbcTemplate.queryForList(
            "SELECT id FROM users WHERE referral_code IS NULL OR referral_code = ''",
            Long::class.java
        )

        println("V11 migration: backfilling referral codes for ${userIds.size} users")

        // Assign a unique referral code to each user
        userIds.forEach { userId ->
            val code = generateUniqueCode(jdbcTemplate)
            jdbcTemplate.update(
                "UPDATE users SET referral_code = ? WHERE id = ?",
                code, userId
            )
        }

        println("V11 migration: completed — all users have referral codes")
    }

    /**
     * Generate an 8-char uppercase alphanumeric code that is guaranteed unique in the DB.
     * Retries if there is a collision (extremely rare).
     */
    private fun generateUniqueCode(jdbcTemplate: JdbcTemplate): String {
        repeat(10) {  // try up to 10 times
            val candidate = UUID.randomUUID().toString()
                .replace("-", "")
                .uppercase()
                .take(8)

            val exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE referral_code = ?",
                Int::class.java,
                candidate
            ) ?: 0

            if (exists == 0) return candidate
        }
        throw IllegalStateException("Could not generate unique referral code after 10 attempts")
    }
}
```

### Registering Kotlin Migrations in application.yml

```yaml
# application.yml

spring:
  flyway:
    locations:
      - classpath:db/migration              # SQL files
      - classpath:com/example/deliveryapp/migration   # Kotlin migration classes
```

### Spring-Bean-Aware Migration (Using @Autowired Services)

When you need an actual Spring bean (like `BCryptPasswordEncoder`) inside a migration, you need a different setup:

```kotlin
// src/main/kotlin/com/example/deliveryapp/config/FlywayConfig.kt

package com.example.deliveryapp.config

import com.example.deliveryapp.migration.V12__encrypt_legacy_passwords
import org.flywaydb.core.Flyway
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import javax.sql.DataSource

@Configuration
class FlywayConfig(
    private val dataSource: DataSource,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val applicationContext: ApplicationContext
) {

    @Bean(initMethod = "migrate")
    fun flyway(): Flyway {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            // Pass Spring beans to migrations via the resolver
            .javaMigrations(
                V12__encrypt_legacy_passwords(passwordEncoder)
            )
            .load()
    }
}
```

```kotlin
// src/main/kotlin/com/example/deliveryapp/migration/V12__encrypt_legacy_passwords.kt

package com.example.deliveryapp.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * Encrypt all legacy plaintext passwords using BCrypt.
 * This migration requires a Spring bean (BCryptPasswordEncoder),
 * so it is injected via the constructor and registered manually in FlywayConfig.
 */
class V12__encrypt_legacy_passwords(
    private val passwordEncoder: BCryptPasswordEncoder
) : BaseJavaMigration() {

    override fun migrate(context: Context) {
        val jdbcTemplate = JdbcTemplate(
            SingleConnectionDataSource(context.connection, true)
        )

        // Find users with plaintext passwords (legacy field)
        data class LegacyUser(val id: Long, val plaintextPassword: String)

        val legacyUsers = jdbcTemplate.query(
            "SELECT id, legacy_password FROM users WHERE legacy_password IS NOT NULL"
        ) { rs, _ ->
            LegacyUser(rs.getLong("id"), rs.getString("legacy_password"))
        }

        println("V12 migration: encrypting ${legacyUsers.size} legacy passwords")

        legacyUsers.forEach { user ->
            val hashed = passwordEncoder.encode(user.plaintextPassword)
            jdbcTemplate.update(
                "UPDATE users SET password_hash = ?, legacy_password = NULL WHERE id = ?",
                hashed, user.id
            )
        }
    }
}
```

---

## 27.9 Testing With Flyway — Real PostgreSQL via Testcontainers

The correct approach is Testcontainers. Your actual migrations run. Your actual schema is used. No mocking. No H2 compatibility issues.

### TestcontainersConfig.kt

```kotlin
// src/test/kotlin/com/example/deliveryapp/TestcontainersConfig.kt

package com.example.deliveryapp

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @ServiceConnection  // Spring Boot auto-configures the DataSource from this container
    fun postgresContainer(): PostgreSQLContainer<*> {
        return PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("deliveryapp_test")
            .withUsername("testuser")
            .withPassword("testpassword")
            // This is what Flyway sees — same as real DB. All your migrations run here.
    }
}
```

### Base Integration Test Class

Create a base class so you don't repeat Testcontainers setup in every test:

```kotlin
// src/test/kotlin/com/example/deliveryapp/AbstractIntegrationTest.kt

package com.example.deliveryapp

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Import(TestcontainersConfig::class)
// Use BEFORE_CLASS so the container starts once for the whole test class
@Transactional  // Each test runs in a transaction that rolls back — keeps DB clean
abstract class AbstractIntegrationTest
```

### Repository Integration Test

```kotlin
// src/test/kotlin/com/example/deliveryapp/repository/OrderRepositoryTest.kt

package com.example.deliveryapp.repository

import com.example.deliveryapp.AbstractIntegrationTest
import com.example.deliveryapp.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

class OrderRepositoryTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Test
    fun `should create order with items and retrieve with correct totals`() {
        // Arrange: create supporting data
        val user = userRepository.save(
            User(
                email = "test@example.com",
                displayName = "Test User",
                referralCode = "TESTCODE"
            )
        )

        val category = categoryRepository.save(
            Category(name = "Beverages", slug = "beverages")
        )

        val product = productRepository.save(
            Product(
                name = "Apple Juice",
                slug = "apple-juice",
                price = BigDecimal("50.00"),
                stockQuantity = 100,
                category = category
            )
        )

        // Act: create an order
        val order = Order(
            user = user,
            totalAmount = BigDecimal("100.00"),
            finalAmount = BigDecimal("90.00"),
            discountAmount = BigDecimal("10.00"),
            deliveryAddress = "123 Main Street, Mumbai"
        )
        order.items.add(
            OrderItem(
                order = order,
                product = product,
                quantity = 2,
                unitPrice = BigDecimal("50.00"),
                totalPrice = BigDecimal("100.00")
            )
        )
        val saved = orderRepository.save(order)

        // Assert
        assertThat(saved.id).isNotNull()
        assertThat(saved.status).isEqualTo(OrderStatus.PENDING)
        assertThat(saved.items).hasSize(1)
        assertThat(saved.items[0].unitPrice).isEqualByComparingTo(BigDecimal("50.00"))
    }

    @Test
    fun `should find orders by user and status`() {
        val user = userRepository.save(
            User(email = "user2@example.com", displayName = "User 2", referralCode = "CODE2222")
        )

        // Create 3 orders in different statuses
        repeat(2) {
            orderRepository.save(
                Order(
                    user = user,
                    totalAmount = BigDecimal("200.00"),
                    finalAmount = BigDecimal("200.00"),
                    deliveryAddress = "456 Test Street"
                )
            )
        }
        val delivered = orderRepository.save(
            Order(
                user = user,
                status = OrderStatus.DELIVERED,
                totalAmount = BigDecimal("300.00"),
                finalAmount = BigDecimal("300.00"),
                deliveryAddress = "789 Delivered Street"
            )
        )

        // Query
        val pendingOrders = orderRepository.findByUserIdAndStatus(user.id!!, OrderStatus.PENDING)
        val deliveredOrders = orderRepository.findByUserIdAndStatus(user.id!!, OrderStatus.DELIVERED)

        assertThat(pendingOrders).hasSize(2)
        assertThat(deliveredOrders).hasSize(1)
        assertThat(deliveredOrders[0].id).isEqualTo(delivered.id)
    }
}
```

### Slice Test With Flyway Disabled

For fast unit-level tests, disable Flyway and use H2:

```kotlin
// src/test/kotlin/com/example/deliveryapp/service/OrderServiceTest.kt

package com.example.deliveryapp.service

import com.example.deliveryapp.entity.*
import com.example.deliveryapp.repository.OrderRepository
import com.example.deliveryapp.repository.ProductRepository
import com.example.deliveryapp.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

// Pure unit test — no Spring context, no database, no Flyway
class OrderServiceTest {

    private val orderRepository: OrderRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val userRepository: UserRepository = mockk()

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository, productRepository, userRepository)
    }

    @Test
    fun `should throw when product is out of stock`() {
        val product = mockk<Product> {
            every { stockQuantity } returns 0
            every { isActive } returns true
            every { id } returns 1L
        }
        every { productRepository.findById(1L) } returns java.util.Optional.of(product)

        assertThatThrownBy {
            orderService.createOrder(userId = 1L, items = listOf(OrderItemRequest(productId = 1L, quantity = 1)))
        }.hasMessageContaining("out of stock")
    }
}
```

### application-test.yml (For @SpringBootTest With H2)

```yaml
# src/test/resources/application-test.yml

spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: none     # Flyway handles schema, not Hibernate

  flyway:
    enabled: true
    locations: classpath:db/migration
    clean-disabled: false  # Allow clean in tests only — wipes DB between test suites
```

---

## 27.10 Adding Flyway to an Existing Database (Baseline)

You have a project that already has a database with tables. You want to add Flyway. The problem: your V1 creates tables that already exist. Flyway will try to run V1 and fail with `relation "users" already exists`.

### The Baseline Pattern — Step by Step

**Step 1:** Write `V1__existing_schema.sql` that matches exactly what is in your current production database. This is documentation of the current state, not a change.

```sql
-- V1__existing_schema.sql
-- This is the baseline: it documents the schema that already exists.
-- Flyway will NOT run this file — it will be marked as "already applied".

CREATE TABLE IF NOT EXISTS users ( ... );   -- matches your actual current schema
CREATE TABLE IF NOT EXISTS products ( ... );
```

**Step 2:** Set `baseline-on-migrate: true` in your `application.yml`. Only for the first run.

```yaml
spring:
  flyway:
    baseline-on-migrate: true   # Only needed ONCE, on the first run against existing DB
    baseline-version: 1         # Treat V1 as the baseline (already applied)
    baseline-description: Existing schema before Flyway adoption
```

**Step 3:** Start the app once. Flyway:
- Creates `flyway_schema_history`
- Inserts a row for V1 with `type = BASELINE` (not `SQL`)
- Does NOT execute V1's SQL
- Runs V2, V3, V4... normally

```sql
-- flyway_schema_history after baseline
SELECT version, description, type, success FROM flyway_schema_history;

-- version | description                             | type     | success
-- --------+-----------------------------------------+----------+--------
-- 1       | Existing schema before Flyway adoption  | BASELINE | true
-- 2       | add phone to users                      | SQL      | true
-- 3       | create coupons table                    | SQL      | true
```

**Step 4:** Immediately set `baseline-on-migrate: false` again. Commit that change. It must only run once.

```yaml
spring:
  flyway:
    baseline-on-migrate: false  # Back to false after first run
```

---

## 27.11 Multi-Environment Configuration (dev / staging / prod)

### Environment-Specific application.yml Files

```yaml
# application-dev.yml — local development
spring:
  flyway:
    enabled: true
    locations:
      - classpath:db/migration
      - classpath:db/seed        # seed data only in dev
    clean-disabled: false        # allow flyway:clean in dev (wipe and rebuild)
  jpa:
    show-sql: true               # show SQL in dev

# application-staging.yml
spring:
  flyway:
    enabled: true
    locations:
      - classpath:db/migration   # no seed data in staging
    clean-disabled: true
  jpa:
    show-sql: false

# application-prod.yml
spring:
  flyway:
    enabled: true
    locations:
      - classpath:db/migration
    validate-on-migrate: true
    out-of-order: false
    clean-disabled: true         # NEVER allow clean in production
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
```

### Dev Seed Data — Only Runs Locally

```sql
-- src/main/resources/db/seed/V100__seed_dev_data.sql
--
-- Version 100+ keeps seed migrations far from production migrations.
-- This file only runs in dev profile (seed/ is not in prod locations).

INSERT INTO categories (name, slug, is_active, sort_order) VALUES
    ('Beverages', 'beverages', TRUE, 1),
    ('Snacks', 'snacks', TRUE, 2),
    ('Dairy', 'dairy', TRUE, 3),
    ('Fruits & Vegetables', 'fruits-vegetables', TRUE, 4)
ON CONFLICT (slug) DO NOTHING;  -- Idempotent: safe to run multiple times

INSERT INTO users (email, display_name, referral_code, is_premium) VALUES
    ('dev@example.com', 'Dev User', 'DEVUSER1', FALSE),
    ('premium@example.com', 'Premium User', 'PREMIUM1', TRUE),
    ('admin@example.com', 'Admin User', 'ADMIN001', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO products (name, slug, price, mrp, stock_quantity, category_id, brand, is_active) VALUES
    ('Tropicana Apple Juice 1L', 'tropicana-apple-1l', 89.00, 99.00, 50,
     (SELECT id FROM categories WHERE slug = 'beverages'), 'Tropicana', TRUE),
    ('Lays Classic Salted 52g', 'lays-classic-52g', 20.00, 20.00, 200,
     (SELECT id FROM categories WHERE slug = 'snacks'), 'Lays', TRUE),
    ('Amul Butter 100g', 'amul-butter-100g', 54.00, 56.00, 30,
     (SELECT id FROM categories WHERE slug = 'dairy'), 'Amul', TRUE)
ON CONFLICT (slug) DO NOTHING;
```

---

## 27.12 Flyway Callbacks — Running Logic Before/After Migrations

Flyway supports callbacks that run at specific lifecycle events:

```
beforeMigrate        → runs before any migration
afterMigrate         → runs after all migrations succeed
afterMigrateError    → runs after a migration fails
beforeEachMigrate    → runs before each individual migration
afterEachMigrate     → runs after each individual migration
beforeClean          → runs before flyway:clean
afterClean           → runs after flyway:clean
```

### Kotlin Callback — Logging and Notifications

```kotlin
// src/main/kotlin/com/example/deliveryapp/migration/FlywayMigrationCallback.kt

package com.example.deliveryapp.migration

import org.flywaydb.core.api.callback.Callback
import org.flywaydb.core.api.callback.Context
import org.flywaydb.core.api.callback.Event
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component  // Spring picks this up and Flyway discovers it automatically
class FlywayMigrationCallback : Callback {

    private val log = LoggerFactory.getLogger(FlywayMigrationCallback::class.java)

    override fun supports(event: Event, context: Context): Boolean {
        // Return true for the events you want to handle
        return event in listOf(
            Event.BEFORE_MIGRATE,
            Event.AFTER_MIGRATE,
            Event.AFTER_MIGRATE_ERROR,
            Event.BEFORE_EACH_MIGRATE,
            Event.AFTER_EACH_MIGRATE
        )
    }

    override fun canHandleInTransaction(event: Event, context: Context): Boolean = true

    override fun handle(event: Event, context: Context) {
        when (event) {
            Event.BEFORE_MIGRATE -> {
                log.info("=== Flyway: Starting database migration ===")
            }
            Event.BEFORE_EACH_MIGRATE -> {
                log.info("Flyway: Running migration → ${context.migrationInfo?.script}")
            }
            Event.AFTER_EACH_MIGRATE -> {
                val info = context.migrationInfo
                log.info("Flyway: ✅ Completed migration → ${info?.script} " +
                         "(${info?.executionTime}ms)")
            }
            Event.AFTER_MIGRATE -> {
                log.info("=== Flyway: All migrations applied successfully ===")
            }
            Event.AFTER_MIGRATE_ERROR -> {
                log.error("=== Flyway: MIGRATION FAILED — check logs above ===")
                // Could send a Slack alert, PagerDuty alert, etc.
            }
            else -> { /* no-op */ }
        }
    }
}
```

---

## 27.13 Common Errors and Exact Fixes

### Error 1: Checksum Mismatch (You Edited a Past Migration)

```
FlywayValidateException: Validate failed:
Migration checksum mismatch for migration version 3
-> Applied to database : 892034512
-> Resolved locally    : -1453219832
```

**Cause:** You edited `V3__something.sql` after it ran.

**Fix in development only** (never in production):

```sql
-- Option A: Update the stored checksum to match your edited file
-- (get the new checksum number from the error message)
UPDATE flyway_schema_history
SET checksum = -1453219832
WHERE version = '3';

-- Option B: Delete the history record so Flyway re-runs the file
-- (only safe if the migration is idempotent — uses IF NOT EXISTS / IF EXISTS)
DELETE FROM flyway_schema_history WHERE version = '3';
```

**Fix in production:** Create a new migration `V{next}__fix_v3_issue.sql` with the correct DDL. Never touch the history table in production.

### Error 2: Migration Not Running (Silent Ignore)

```
# You created V4__add_coupon_table.sql
# App starts, no error, but the coupons table doesn't exist
```

**Diagnosis:**
```yaml
# Add to application.yml temporarily
logging:
  level:
    org.flywaydb: DEBUG
```

Look for: `Skipping: V4__add_coupon_table.sql` in the logs. Then check:
- Is it double underscore? `V4__` not `V4_`
- Is `V` uppercase?
- Is extension `.sql` lowercase?
- Is there a space in the description?

### Error 3: Migration Failed Halfway (Table Half Created)

```
FlywayException: Migration V5__create_coupons.sql failed
SQL State: 42P07
Message: relation "coupons" already exists
```

**Cause:** V5 ran partially (the first CREATE TABLE succeeded, then something else failed). On retry, the first CREATE TABLE fails because the table already exists.

**Fix:**
```sql
-- 1. Remove the failed migration from history (it has success=false)
DELETE FROM flyway_schema_history WHERE version = '5' AND success = false;

-- 2. Clean up any partial state
DROP TABLE IF EXISTS coupon_uses;
DROP TABLE IF EXISTS coupons;

-- 3. Fix your V5 SQL to use IF NOT EXISTS on all CREATE TABLE statements
-- 4. Restart the app — V5 will run again
```

**Prevention:** Always write `CREATE TABLE IF NOT EXISTS` and `ADD COLUMN IF NOT EXISTS`.

### Error 4: Schema Validation Failure (Entity/Migration Mismatch)

```
SchemaManagementException: Schema-validation: missing column [display_name] in table [users]
```

**Cause:** You added `displayName` to your `User` entity but forgot to write the migration.

**Fix:**
```sql
-- Create the missing migration file
-- V{next}__add_display_name_to_users.sql

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);

UPDATE users SET display_name = full_name WHERE display_name IS NULL;

ALTER TABLE users ALTER COLUMN display_name SET NOT NULL;
```

### Error 5: Out-of-Order Migration

```
FlywayException: Detected resolved migration not applied to database: Version 6
```

**Cause:** Two developers on different branches both created migrations. Developer A's V6 was merged first. Developer B's V6 (which is a different file) now conflicts.

**Fix:** Rename the conflicting file:

```bash
# Before (conflict):
git mv V6__developer_b_feature.sql V8__developer_b_feature.sql
git commit -m "Rename migration to resolve version conflict"
```

Or if you must allow it temporarily:

```yaml
spring:
  flyway:
    out-of-order: true  # Temporarily allow. Return to false after all environments sync.
```

---

## 27.14 Zero-Downtime Migration Patterns for Production

In production with rolling deploys, you cannot take downtime to run migrations. Your new code and old code may run simultaneously against the same database. Follow these patterns:

### Pattern 1: Expand-Contract (The Standard Approach)

**Expanding (backward-compatible) changes** are safe to deploy with zero downtime:
- Adding a new nullable column
- Adding a new table
- Adding an index (`CONCURRENTLY` to avoid locking)
- Adding a constraint with `NOT VALID` (validates later)

**Contracting (breaking) changes** require multiple deployments:
- Dropping a column
- Renaming a column
- Making a nullable column NOT NULL
- Changing a column type

### Pattern 2: Adding a NOT NULL Column Safely

```sql
-- V{n}__add_not_null_column_step1.sql
-- Deploy with old code: add nullable first

ALTER TABLE users ADD COLUMN IF NOT EXISTS region VARCHAR(50);
```

```sql
-- V{n+1}__add_not_null_column_step2.sql
-- Deploy AFTER new code is shipped and all rows have values

-- Add NOT NULL constraint AFTER backfilling
-- NOT VALID means: don't scan existing rows yet
ALTER TABLE users
    ADD CONSTRAINT chk_users_region_not_null
    CHECK (region IS NOT NULL) NOT VALID;

-- Validate separately (doesn't lock the table)
ALTER TABLE users
    VALIDATE CONSTRAINT chk_users_region_not_null;

-- Now safely set NOT NULL (already guaranteed by constraint)
ALTER TABLE users ALTER COLUMN region SET NOT NULL;
DROP CONSTRAINT chk_users_region_not_null;
```

### Pattern 3: Creating Indexes Without Locking

```sql
-- V{n}__add_index_concurrently.sql
-- CONCURRENTLY means the index builds without locking the table for writes.
-- Takes longer but does not block production traffic.

-- Note: CONCURRENTLY cannot run inside a transaction, so this must be a
-- standalone migration file (Flyway runs it in a separate connection).

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_user_status
    ON orders(user_id, status)
    WHERE status IN ('PENDING', 'CONFIRMED', 'PREPARING');
```

> **Important:** `CREATE INDEX CONCURRENTLY` cannot run inside a transaction. If your Flyway migration is transactional by default, you need to disable the transaction for this migration. Create a Kotlin-based migration:

```kotlin
// V13__add_concurrent_index.kt

package com.example.deliveryapp.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V13__add_concurrent_index : BaseJavaMigration() {

    // This tells Flyway to NOT wrap this migration in a transaction
    override fun canExecuteInTransaction(): Boolean = false

    override fun migrate(context: Context) {
        context.connection.createStatement().use { stmt ->
            stmt.execute("""
                CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_user_status
                ON orders(user_id, status)
                WHERE status IN ('PENDING', 'CONFIRMED', 'PREPARING')
            """.trimIndent())
        }
    }
}
```

---

## 27.15 Flyway CLI and Gradle Tasks

### Flyway Gradle Plugin Commands

```bash
# Show the status of all migrations
./gradlew flywayInfo

# Output:
# +------------+---------+---------------------+------+---------------------+---------+----------+
# | Category   | Version | Description         | Type | Installed On        | State   | Undoable |
# +------------+---------+---------------------+------+---------------------+---------+----------+
# | Versioned  | 1       | create initial...   | SQL  | 2024-01-15 10:00:00 | Success | No       |
# | Versioned  | 2       | create indexes      | SQL  | 2024-01-15 10:00:01 | Success | No       |
# | Versioned  | 3       | add phone to users  | SQL  | 2024-01-20 14:30:00 | Success | No       |
# | Versioned  | 4       | create coupons      | SQL  |                     | Pending | No       |  ← not yet run
# +------------+---------+---------------------+------+---------------------+---------+----------+

# Run all pending migrations
./gradlew flywayMigrate

# Validate checksums (no DB changes)
./gradlew flywayValidate

# Fix failed migrations in history table
./gradlew flywayRepair

# DROP ALL TABLES (ONLY in dev, NEVER production)
./gradlew flywayClean
```

### Viewing Migration State From Kotlin Code at Runtime

```kotlin
// src/main/kotlin/com/example/deliveryapp/controller/AdminController.kt

package com.example.deliveryapp.controller

import org.flywaydb.core.Flyway
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/flyway")
class AdminController(private val flyway: Flyway) {

    /**
     * GET /admin/flyway/status
     * Returns the status of all migrations.
     * Useful for health checks and dashboards.
     */
    @GetMapping("/status")
    fun migrationStatus(): List<MigrationInfo> {
        return flyway.info().all().map { info ->
            MigrationInfo(
                version = info.version?.version ?: "repeatable",
                description = info.description,
                state = info.state.name,
                installedOn = info.installedOn?.toString(),
                executionTime = info.executionTime,
                checksum = info.checksum
            )
        }
    }

    data class MigrationInfo(
        val version: String,
        val description: String,
        val state: String,
        val installedOn: String?,
        val executionTime: Int?,
        val checksum: Int?
    )
}
```

---

## 27.16 The Golden Rules — Never Break These

| Rule | Wrong | Correct |
|---|---|---|
| Past migrations are frozen | Edit `V3__add_column.sql` | Create `V8__fix_v3_column.sql` |
| Never delete a file | Delete `V2__indexes.sql` | Create `V9__drop_old_indexes.sql` |
| No duplicate versions | Two files named `V4__...` | Rename one to `V5__...` |
| ddl-auto in production | `ddl-auto: create` | `ddl-auto: validate` |
| One concern per file | Schema + data in one file | Separate DDL and DML files |
| Always use IF NOT EXISTS | `CREATE TABLE users` | `CREATE TABLE IF NOT EXISTS users` |
| Never flyway:clean in prod | `./gradlew flywayClean` (prod) | `clean-disabled: true` in prod config |

---

## 27.17 Quick Reference Cheat Sheet

### File Naming

```
V{number}__{description}.sql    → Versioned: runs once
R__{description}.sql            → Repeatable: re-runs when content changes
V{n}__description.kt            → Kotlin programmatic migration
```

### Every SQL Migration Pattern

```sql
-- Create table
CREATE TABLE IF NOT EXISTS table_name (...);

-- Add column (nullable)
ALTER TABLE t ADD COLUMN IF NOT EXISTS col_name TYPE;

-- Add column (NOT NULL with default)
ALTER TABLE t ADD COLUMN IF NOT EXISTS col_name TYPE NOT NULL DEFAULT value;

-- Drop column
ALTER TABLE t DROP COLUMN IF EXISTS col_name;

-- Rename column (use expand-contract for zero downtime)
ALTER TABLE t RENAME COLUMN old_name TO new_name;

-- Add index
CREATE INDEX IF NOT EXISTS idx_name ON table(column);

-- Add index without locking (production-safe)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_name ON table(column);

-- Add unique index
CREATE UNIQUE INDEX IF NOT EXISTS idx_name ON table(column);

-- Add check constraint
ALTER TABLE t ADD CONSTRAINT constraint_name CHECK (condition);

-- Add foreign key
ALTER TABLE t ADD CONSTRAINT fk_name FOREIGN KEY (col) REFERENCES other(id);

-- Data backfill (always idempotent)
UPDATE table SET col = value WHERE col IS NULL;
```

### Query flyway_schema_history

```sql
-- Check what has run and in what order
SELECT installed_rank, version, description, type, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- Check for failed migrations
SELECT * FROM flyway_schema_history WHERE success = false;

-- Check last migration that ran
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;
```

### Gradle Commands

```bash
./gradlew flywayInfo      # Show all migration states
./gradlew flywayMigrate   # Run pending migrations
./gradlew flywayValidate  # Validate checksums
./gradlew flywayRepair    # Fix failed migration records
./gradlew flywayClean     # DROP EVERYTHING — dev only
```

---

## 27.18 Interview Questions — What You Will Actually Be Asked

**Q: What is Flyway and what problem does it solve?**

Flyway is a database migration tool. It solves the problem of managing schema changes across multiple environments (dev, staging, production) and multiple developers. Without it, each environment's schema drifts apart. Flyway uses versioned SQL files (`V1__...sql`) that run exactly once and are tracked in a `flyway_schema_history` table. Every environment always runs the same migrations in the same order, guaranteeing identical schemas.

**Q: What is the difference between Flyway and `ddl-auto: update`?**

`ddl-auto: update` is Hibernate's auto-schema feature. It looks at your entity classes and tries to make the database match — adding missing columns, creating missing tables. The problems: it never drops columns (only adds), it handles complex changes poorly (foreign keys, indexes, constraints), and you have no control or history of what it did. Flyway gives you full control, a complete history, checksums, and works on every environment identically. In production, always use Flyway with `ddl-auto: validate` (Hibernate checks but never changes).

**Q: How do you handle a database rename in a zero-downtime deployment?**

Use the expand-contract pattern across three deployments:
1. **Expand:** Add the new column. Both old and new columns exist. New code writes to both, reads from new with fallback to old.
2. **Migrate:** Backfill old column values into new column via a data migration.
3. **Contract:** Drop the old column. Only the new column remains.
This ensures at no point is the running code incompatible with the database schema.

**Q: What happens if two developers create V5 on different branches?**

When merged, both files exist as `V5__...sql`. On startup, Flyway detects two files with version 5 and throws an exception. The fix: rename one file to V6 (or whichever is next), update the description to reflect what it actually does, and merge again. The team should establish a policy (e.g., claim migration numbers in a shared document) to avoid this.

**Q: Can you write a migration in Kotlin instead of SQL?**

Yes. Flyway supports Java/Kotlin programmatic migrations by implementing `BaseJavaMigration`. The class naming must follow the Flyway pattern (`V11__description`). This is used when you need application logic during migration — calling an external service, using Spring beans like `BCryptPasswordEncoder`, or batch-processing data with complex Kotlin logic that would be verbose in SQL. You register the location in `spring.flyway.locations` as a classpath package.

**Q: What does `validate-on-migrate: true` do?**

Every time the app starts, Flyway recomputes the CRC32 checksum of every migration file that has already run and compares it to the stored checksum. If any file was modified after it ran, Flyway throws a `FlywayValidateException` and the app refuses to start. This prevents silent corruption of the migration history. It should always be `true`.

**Q: How do you add Flyway to a project that already has a database?**

Use the baseline approach. Write `V1__existing_schema.sql` that documents the current schema. Set `baseline-on-migrate: true` and `baseline-version: 1`. On first run, Flyway creates the history table and marks V1 as a BASELINE record (type = BASELINE) without running it. Future migrations (V2, V3, ...) run normally. After the first run, immediately set `baseline-on-migrate: false` so it does not repeat.

**Q: What are the pros and cons of Flyway vs ddl-auto: update?**

`ddl-auto: update` feels convenient — Hibernate auto-syncs your entity classes to the database. But it is dangerous in production because: it can only add columns and tables, never drop or rename them (a rename creates a ghost column and loses all existing data), it leaves no history of changes, there is no checksum protection, and different environments silently diverge. Flyway forces you to write explicit SQL, which is more work upfront but gives you complete control, a full audit trail, checksums, and identical environments. The extra 5 minutes writing a migration file saves hours of debugging production data corruption.

**Q: Why does IntelliJ's generated SQL need manual review before using in migrations?**

IntelliJ generates SQL based on its interpretation of your entity classes, but it cannot know your intent. It may infer the wrong data type (choosing `BIGINT` when you need `INTEGER`), miss performance indexes on foreign key columns, omit `CHECK` constraints, skip `NOT NULL` defaults for existing rows, or use syntax that doesn't match your target PostgreSQL version. The generated SQL is a useful starting draft — read it, edit it, add missing pieces, test it locally, then commit the reviewed version as your migration file.

**Q: What is a repeatable migration (R__ prefix) and when do you use it?**

Repeatable migrations have no version number. They re-run every time their content changes (Flyway recomputes their checksum). They always run after all versioned migrations. They are ideal for database objects that you want to fully replace rather than patch: views (`CREATE OR REPLACE VIEW`), stored functions (`CREATE OR REPLACE FUNCTION`), and stored procedures. For example, if you change a reporting view, you just edit the `R__create_views.sql` file — Flyway sees the checksum changed and re-runs it, replacing the old view definition automatically.

---

*Next Chapter: [[Chapter 3 — Repositories and Queries]]*
*Previous Chapter: [[Chapter 26 — Kotlin Multithreading, Coroutines and Async Spring Boot]]*
