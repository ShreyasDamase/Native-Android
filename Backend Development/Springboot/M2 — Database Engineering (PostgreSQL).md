# M2 — Database Engineering (PostgreSQL)

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Scope**: Every schema, migration, entity, repository, and concurrency pattern you need to build a production e-commerce/quick-commerce backend (Zepto/Blinkit-style). This chapter is not about CRUD — it is about data integrity, consistency, and the database decisions that determine whether your system survives Black Friday traffic.

---

## Table of Contents

1. [User Schema](#21-user-schema)
2. [Product & Category Schema](#22-product--category-schema)
3. [Inventory Schema](#23-inventory-schema)
4. [Order & Order Item Schema](#24-order--order-item-schema)
5. [Wallet & Ledger Schema](#25-wallet--ledger-schema)
6. [Transactions, Locking, Concurrency](#26-transactions-locking-concurrency)

---

## Flyway: The Migration Philosophy

Before writing a single table, understand Flyway's role:

- **Flyway owns the schema, not Hibernate**. Set `spring.jpa.hibernate.ddl-auto: none` always. If you let Hibernate manage DDL (`create-drop`, `update`), it will happily drop columns in production.
- Migrations are **immutable** once applied. Never edit `V1__create_users_table.sql` after it's run in any environment. Create `V2__add_column.sql` instead.
- `validate-on-migrate: true` checksums every migration file. If you edit an applied migration, Flyway fails at startup — this is protection, not a bug.
- Migration naming convention: `V{version}__{description}.sql` — version is an integer or semantic version, description uses underscores.

```
src/main/resources/
└── db/
    └── migration/
        ├── V1__create_users_table.sql
        ├── V2__create_categories_and_products.sql
        ├── V3__create_inventory_tables.sql
        ├── V4__create_orders_tables.sql
        └── V5__create_wallet_tables.sql
```

> [!WARNING]
> Never use `flyway.baseline-on-migrate: true` in production. This tells Flyway to assume all existing tables are already at baseline, skipping migrations on a non-empty DB. If you accidentally run this against a prod DB that's behind on migrations, Flyway silently does nothing and your schema is now inconsistent with your code. This has caused real production outages.

---

## 2.1 User Schema

### UUID vs BIGSERIAL — The Distributed Systems Decision

| Aspect | UUID (v4) | BIGSERIAL |
|--------|-----------|-----------|
| Uniqueness across shards | ✅ Globally unique | ❌ Unique only per shard |
| ID leaking (enumeration attack) | ✅ Unpredictable | ❌ Sequential IDs are guessable |
| Index performance | ⚠️ Random UUIDs cause page splits | ✅ Sequential — cache-friendly |
| JOIN performance | Slightly slower (16 bytes vs 8) | Faster (8 bytes) |
| URL safety | ✅ Standard | ✅ Standard |
| Merge across environments | ✅ No collision | ❌ Collision risk |

**Decision for a quick-commerce startup**: Use UUID v7 (time-ordered) or `gen_random_uuid()` (v4). UUID v4's random insertion pattern causes B-tree page splits (write amplification), but on modern SSDs and with PostgreSQL 14+ this is acceptable. The security and distribution benefits outweigh the index cost at startup scale. At Uber-scale (hundreds of millions of rows), consider `ULID` or UUID v7 for sequential UUID performance with global uniqueness.

> [!NOTE]
> PostgreSQL 13+ has `gen_random_uuid()` built-in. For UUID v7 (time-ordered, solves the index fragmentation problem), use the `pg_uuidv7` extension or generate in application code.

### `V1__create_users_table.sql`

```sql
-- V1__create_users_table.sql
-- Enabling UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- User status and role as PostgreSQL enums
-- Prefer DB-level enums over varchar for type safety and storage efficiency
CREATE TYPE user_status AS ENUM (
    'PENDING_VERIFICATION',
    'ACTIVE',
    'SUSPENDED',
    'DELETED'
);

CREATE TYPE user_role AS ENUM (
    'CUSTOMER',
    'ADMIN',
    'DELIVERY_PARTNER',
    'WAREHOUSE_STAFF'
);

CREATE TABLE users (
    -- UUID primary key — not auto-increment
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    
    -- Personal info
    first_name          VARCHAR(50)     NOT NULL,
    last_name           VARCHAR(50)     NOT NULL,
    email               VARCHAR(255)    NOT NULL,
    phone_number        VARCHAR(20)     NOT NULL,
    password_hash       TEXT            NOT NULL,
    
    -- Access control
    role                user_role       NOT NULL DEFAULT 'CUSTOMER',
    status              user_status     NOT NULL DEFAULT 'PENDING_VERIFICATION',
    
    -- Soft delete
    -- Why soft delete? Regulatory compliance (user data must be retainable for X years),
    -- audit trail, foreign key referential integrity (orders reference user_id)
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    
    -- Optimistic locking
    version             BIGINT          NOT NULL DEFAULT 0,
    
    -- Timestamps — always TIMESTAMPTZ (with timezone), never TIMESTAMP (without timezone)
    -- TIMESTAMP stores local time without context. TIMESTAMPTZ stores UTC + DST-safe.
    -- A system using TIMESTAMP will silently produce wrong results during DST transitions.
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

-- ── Indexes ────────────────────────────────────────────────────────────────────

-- Unique constraint on email — the DB-level safety net even if app-level check fails
-- Partial index: only non-deleted users need email uniqueness
-- A deleted user's email should be reusable (GDPR "right to erasure" → re-registration)
CREATE UNIQUE INDEX idx_users_email_unique 
    ON users (email) 
    WHERE is_deleted = FALSE;

-- Same for phone number
CREATE UNIQUE INDEX idx_users_phone_unique 
    ON users (phone_number) 
    WHERE is_deleted = FALSE;

-- Composite index for status + time range queries (admin dashboards, analytics)
-- Column order matters: equality filter (status) first, then range filter (created_at)
-- This index satisfies: WHERE status = 'ACTIVE' AND created_at > '2024-01-01'
CREATE INDEX idx_users_status_created_at 
    ON users (status, created_at DESC) 
    WHERE is_deleted = FALSE;

-- ── Trigger: auto-update updated_at ───────────────────────────────────────────
-- PostgreSQL does not auto-update timestamps like MySQL's ON UPDATE CURRENT_TIMESTAMP
-- This trigger handles it at the DB level — even for direct SQL updates bypassing the ORM
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_updated_at_trigger
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ── Comments for DBA visibility ────────────────────────────────────────────────
COMMENT ON TABLE users IS 'Core user accounts table. Soft delete pattern — is_deleted=true retains data for audit.';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash, cost factor 12. Never store plaintext.';
COMMENT ON COLUMN users.version IS 'Optimistic locking version. Incremented by Hibernate on each UPDATE.';
```

### User Entity (Updated for DB Enum Types)

```kotlin
// entity/User.kt
package com.zepto.backend.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    var id: UUID? = null

    @Column(name = "first_name", nullable = false, length = 50)
    var firstName: String = ""

    @Column(name = "last_name", nullable = false, length = 50)
    var lastName: String = ""

    @Column(name = "email", nullable = false, length = 255)
    var email: String = ""

    @Column(name = "phone_number", nullable = false, length = 20)
    var phoneNumber: String = ""

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = ""

    // columnDefinition maps to PostgreSQL enum type
    // EnumType.STRING stores the enum name as text — compatible with PG enums
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    var role: UserRole = UserRole.CUSTOMER

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "user_status")
    var status: UserStatus = UserStatus.PENDING_VERIFICATION

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
    override fun toString(): String = "User(id=$id, email=$email)"
}

enum class UserRole { CUSTOMER, ADMIN, DELIVERY_PARTNER, WAREHOUSE_STAFF }
enum class UserStatus { PENDING_VERIFICATION, ACTIVE, SUSPENDED, DELETED }
```

---

## 2.2 Product & Category Schema

### Category Tree with Self-Reference

Quick-commerce product catalogs are hierarchical: `Electronics > Phones > Smartphones`. A self-referencing `parent_id` FK models this with a single table. The depth is typically shallow (3-4 levels), making the recursive query cost negligible.

### `V2__create_categories_and_products.sql`

```sql
-- V2__create_categories_and_products.sql

-- ── Categories ─────────────────────────────────────────────────────────────────
CREATE TABLE categories (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(100)    NOT NULL,
    slug            VARCHAR(100)    NOT NULL,   -- URL-friendly: "fresh-vegetables"
    description     TEXT,
    image_url       TEXT,
    
    -- Self-referencing FK for tree structure
    -- NULL = root category (no parent)
    parent_id       UUID            REFERENCES categories(id) ON DELETE RESTRICT,
    
    -- Ordering within the same parent for UI display
    sort_order      INT             NOT NULL DEFAULT 0,
    
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT categories_pkey PRIMARY KEY (id)
);

-- Slug must be unique globally for URL routing
CREATE UNIQUE INDEX idx_categories_slug ON categories (slug);

-- For fetching all children of a parent
CREATE INDEX idx_categories_parent_id ON categories (parent_id);
CREATE INDEX idx_categories_active ON categories (is_active) WHERE is_active = TRUE;

CREATE TRIGGER categories_updated_at_trigger
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── Products ───────────────────────────────────────────────────────────────────
CREATE TABLE products (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    
    -- URL slug: "amul-butter-500g" — must be unique for SEO-friendly routing
    slug            VARCHAR(255)    NOT NULL,
    
    description     TEXT,
    
    -- DECIMAL(precision, scale) — (12, 2) supports up to 9,999,999,999.99
    -- NEVER use FLOAT or DOUBLE for money — floating point cannot represent 0.10 exactly
    base_price      DECIMAL(12, 2)  NOT NULL CHECK (base_price >= 0),
    
    -- Selling price may differ from base price (after discount)
    selling_price   DECIMAL(12, 2)  NOT NULL CHECK (selling_price >= 0),
    
    -- Percentage: 0.00 to 100.00
    discount_percent DECIMAL(5, 2) NOT NULL DEFAULT 0 CHECK (discount_percent BETWEEN 0 AND 100),
    
    category_id     UUID            NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    
    -- Metadata
    brand           VARCHAR(100),
    unit            VARCHAR(20)     NOT NULL DEFAULT 'piece',   -- "kg", "litre", "piece"
    weight_grams    INT,            -- For delivery weight calculation
    
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    is_featured     BOOLEAN         NOT NULL DEFAULT FALSE,
    
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT products_pkey PRIMARY KEY (id),
    
    -- DB-level constraint: selling price must not exceed base price
    CONSTRAINT check_selling_price CHECK (selling_price <= base_price)
);

CREATE UNIQUE INDEX idx_products_slug ON products (slug);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_active ON products (is_active, category_id) WHERE is_active = TRUE;
CREATE INDEX idx_products_featured ON products (is_featured) WHERE is_featured = TRUE;

-- Full text search index for product search
-- GIN index on tsvector for fast text search
CREATE INDEX idx_products_search ON products 
    USING GIN (to_tsvector('english', name || ' ' || COALESCE(description, '') || ' ' || COALESCE(brand, '')));

CREATE TRIGGER products_updated_at_trigger
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── Product Images ─────────────────────────────────────────────────────────────
-- Separate table — not array column or JSON.
-- Why? Arrays can't be independently indexed, queried, or have their own metadata.
-- If you need to reorder, add alt text, or mark primary, you need a separate table.
CREATE TABLE product_images (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    product_id      UUID            NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url             TEXT            NOT NULL,
    alt_text        VARCHAR(255),
    sort_order      INT             NOT NULL DEFAULT 0,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT product_images_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

-- Only one primary image per product
CREATE UNIQUE INDEX idx_product_images_primary 
    ON product_images (product_id) 
    WHERE is_primary = TRUE;
```

### Category Entity with JPA Tree

```kotlin
// entity/Category.kt
package com.zepto.backend.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "categories")
@EntityListeners(AuditingEntityListener::class)
class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    @Column(name = "slug", nullable = false, length = 100)
    var slug: String = ""

    @Column(name = "description")
    var description: String? = null

    @Column(name = "image_url")
    var imageUrl: String? = null

    // Self-referencing parent — LAZY to avoid N+1 when loading leaf categories
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null

    // Children — use Set not List for better Hibernate merge behavior
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    var children: MutableSet<Category> = mutableSetOf()

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Category) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
```

```kotlin
// entity/Product.kt
package com.zepto.backend.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener::class)
class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "slug", nullable = false, length = 255)
    var slug: String = ""

    @Column(name = "description")
    var description: String? = null

    // BigDecimal — never Double or Float for money
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    var basePrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    var sellingPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    var discountPercent: BigDecimal = BigDecimal.ZERO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category? = null

    @Column(name = "brand", length = 100)
    var brand: String? = null

    @Column(name = "unit", nullable = false, length = 20)
    var unit: String = "piece"

    @Column(name = "weight_grams")
    var weightGrams: Int? = null

    // Images — CascadeType.ALL + orphanRemoval so adding/removing images manages the child table
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    var images: MutableList<ProductImage> = mutableListOf()

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "is_featured", nullable = false)
    var isFeatured: Boolean = false

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Product) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

@Entity
@Table(name = "product_images")
class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null

    @Column(name = "url", nullable = false)
    var url: String = ""

    @Column(name = "alt_text", length = 255)
    var altText: String? = null

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProductImage) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
```

### Product Repository with Full-Text Search

```kotlin
// repository/ProductRepository.kt
package com.zepto.backend.repository

import com.zepto.backend.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ProductRepository : JpaRepository<Product, UUID> {

    fun findBySlugAndIsActiveTrue(slug: String): Optional<Product>

    fun findByCategoryIdAndIsActiveTrue(categoryId: UUID, pageable: Pageable): Page<Product>

    // Full-text search using PostgreSQL's native tsvector
    // plainto_tsquery converts plain text to tsquery (no need for user to know TS syntax)
    @Query(value = """
        SELECT p.* FROM products p
        WHERE p.is_active = TRUE
        AND to_tsvector('english', p.name || ' ' || COALESCE(p.description, '') || ' ' || COALESCE(p.brand, ''))
            @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(
            to_tsvector('english', p.name || ' ' || COALESCE(p.description, '') || ' ' || COALESCE(p.brand, '')),
            plainto_tsquery('english', :searchTerm)
        ) DESC
    """, nativeQuery = true)
    fun searchProducts(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Product>

    // Category tree query — get all products in a category and all its descendants
    // Uses PostgreSQL recursive CTE
    @Query(value = """
        WITH RECURSIVE category_tree AS (
            SELECT id FROM categories WHERE id = :categoryId
            UNION ALL
            SELECT c.id FROM categories c
            INNER JOIN category_tree ct ON c.parent_id = ct.id
        )
        SELECT p.* FROM products p
        WHERE p.category_id IN (SELECT id FROM category_tree)
        AND p.is_active = TRUE
    """, nativeQuery = true)
    fun findAllInCategoryTree(@Param("categoryId") categoryId: UUID, pageable: Pageable): Page<Product>
}
```

---

## 2.3 Inventory Schema

### The Inventory Problem — Why It's Hard

Inventory is the hardest part of e-commerce engineering. Zepto's 10-minute delivery promise means inventory data must be real-time accurate. The naive approach (just a `quantity` column) fails because:

1. **Race condition**: Two users check stock simultaneously — both see 1 unit available — both place orders — you've oversold.
2. **No audit trail**: You cannot reconstruct why inventory is at a certain level.
3. **No reservation**: Stock is committed before payment — if payment fails, you need to release stock.

The correct model has three layers:
- `inventory`: Current state (total, reserved, available)
- `inventory_movements`: Immutable audit log of every change
- `inventory_reservations`: Time-limited holds during checkout

### `V3__create_inventory_tables.sql`

```sql
-- V3__create_inventory_tables.sql

-- Warehouses (dark stores for quick-commerce)
CREATE TABLE warehouses (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(100)    NOT NULL,
    code            VARCHAR(20)     NOT NULL,  -- e.g., "MUM-001", "BLR-002"
    address         TEXT            NOT NULL,
    city            VARCHAR(100)    NOT NULL,
    pincode         VARCHAR(10)     NOT NULL,
    -- Geolocation for finding nearest warehouse to delivery address
    latitude        DECIMAL(9, 6),
    longitude       DECIMAL(9, 6),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT warehouses_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_warehouses_code ON warehouses (code);

-- ── Inventory ──────────────────────────────────────────────────────────────────
CREATE TABLE inventory (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    product_id          UUID        NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id        UUID        NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    
    -- Total physically in warehouse (including reserved units)
    total_quantity      INT         NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
    
    -- Units held by active reservations (pending checkout)
    reserved_quantity   INT         NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    
    -- available = total - reserved (maintained by triggers, not application)
    -- Computed column would be cleaner but requires PostgreSQL 12+ generated columns
    -- We maintain it manually + constraint to ensure consistency
    available_quantity  INT         NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    
    -- Reorder alert threshold
    low_stock_threshold INT         NOT NULL DEFAULT 10,
    
    -- Optimistic locking for inventory updates
    version             BIGINT      NOT NULL DEFAULT 0,
    
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT inventory_pkey PRIMARY KEY (id),
    CONSTRAINT inventory_product_warehouse_unique UNIQUE (product_id, warehouse_id),
    
    -- Consistency constraint: available = total - reserved
    CONSTRAINT check_available_quantity 
        CHECK (available_quantity = total_quantity - reserved_quantity)
);

CREATE INDEX idx_inventory_product_id ON inventory (product_id);
CREATE INDEX idx_inventory_warehouse_id ON inventory (warehouse_id);
CREATE INDEX idx_inventory_low_stock ON inventory (warehouse_id, product_id) 
    WHERE available_quantity <= low_stock_threshold;

-- ── Inventory Movements — Immutable Audit Log ─────────────────────────────────
CREATE TYPE movement_type AS ENUM (
    'PURCHASE_RECEIPT',     -- Stock received from supplier
    'SALE',                 -- Stock sold (order placed)
    'RETURN',               -- Customer return
    'DAMAGE',               -- Damaged goods written off
    'ADJUSTMENT',           -- Manual adjustment (stock take)
    'TRANSFER_IN',          -- Transfer from another warehouse
    'TRANSFER_OUT',         -- Transfer to another warehouse
    'RESERVATION',          -- Stock reserved for pending order
    'RESERVATION_RELEASE'   -- Reservation released (order cancelled/expired)
);

CREATE TABLE inventory_movements (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    inventory_id    UUID            NOT NULL REFERENCES inventory(id) ON DELETE RESTRICT,
    product_id      UUID            NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id    UUID            NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    
    type            movement_type   NOT NULL,
    
    -- Positive = stock in, Negative = stock out
    quantity_delta  INT             NOT NULL,  -- Can be negative
    
    -- Snapshot of inventory state after this movement
    quantity_before INT             NOT NULL,
    quantity_after  INT             NOT NULL,
    
    -- What caused this movement
    reference_type  VARCHAR(50),    -- "ORDER", "PURCHASE_ORDER", "MANUAL", "TRANSFER"
    reference_id    UUID,           -- The order_id, purchase_order_id, etc.
    
    notes           TEXT,
    created_by      UUID            REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    -- Movements are immutable — no updates, no deletes (regulatory requirement)
    CONSTRAINT inventory_movements_pkey PRIMARY KEY (id),
    CONSTRAINT check_quantity_consistency 
        CHECK (quantity_after = quantity_before + quantity_delta)
);

-- For querying movement history of a product
CREATE INDEX idx_inv_movements_inventory_id ON inventory_movements (inventory_id, created_at DESC);
CREATE INDEX idx_inv_movements_reference ON inventory_movements (reference_type, reference_id);

-- ── Inventory Reservations ─────────────────────────────────────────────────────
CREATE TYPE reservation_status AS ENUM (
    'PENDING',      -- Active hold — stock reserved during checkout
    'COMMITTED',    -- Order confirmed — becomes a SALE movement
    'RELEASED',     -- Released — checkout abandoned, payment failed, order cancelled
    'EXPIRED'       -- Expired — cleanup job sets this when expires_at is past
);

CREATE TABLE inventory_reservations (
    id              UUID                NOT NULL DEFAULT gen_random_uuid(),
    product_id      UUID                NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    warehouse_id    UUID                NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    order_id        UUID,               -- NULL until order is placed
    user_id         UUID                NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    
    quantity        INT                 NOT NULL CHECK (quantity > 0),
    
    status          reservation_status  NOT NULL DEFAULT 'PENDING',
    
    -- Hold expires after 15 minutes if checkout not completed
    -- Cleanup job runs every minute to RELEASE expired reservations
    expires_at      TIMESTAMPTZ         NOT NULL DEFAULT (NOW() + INTERVAL '15 minutes'),
    
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    
    CONSTRAINT inventory_reservations_pkey PRIMARY KEY (id)
);

-- CRITICAL: Index on expires_at — the cleanup job runs:
-- DELETE FROM inventory_reservations WHERE status = 'PENDING' AND expires_at < NOW()
-- Without this index, a table scan on every cleanup run causes DB CPU spikes
CREATE INDEX idx_inv_reservations_expires ON inventory_reservations (expires_at) 
    WHERE status = 'PENDING';

CREATE INDEX idx_inv_reservations_order_id ON inventory_reservations (order_id);
CREATE INDEX idx_inv_reservations_user_product ON inventory_reservations (user_id, product_id) 
    WHERE status = 'PENDING';

CREATE TRIGGER inventory_reservations_updated_at
    BEFORE UPDATE ON inventory_reservations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### Inventory Entities

```kotlin
// entity/Inventory.kt
package com.zepto.backend.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "inventory")
class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    var warehouse: Warehouse? = null

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int = 0

    @Column(name = "reserved_quantity", nullable = false)
    var reservedQuantity: Int = 0

    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Int = 0

    @Column(name = "low_stock_threshold", nullable = false)
    var lowStockThreshold: Int = 10

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    /**
     * Reserve stock for a checkout session.
     * Throws if insufficient available stock.
     */
    fun reserve(quantity: Int) {
        require(availableQuantity >= quantity) {
            "Insufficient stock. Available: $availableQuantity, Requested: $quantity"
        }
        reservedQuantity += quantity
        availableQuantity -= quantity
    }

    /**
     * Release a previously made reservation.
     */
    fun release(quantity: Int) {
        require(reservedQuantity >= quantity) {
            "Cannot release more than reserved. Reserved: $reservedQuantity, Releasing: $quantity"
        }
        reservedQuantity -= quantity
        availableQuantity += quantity
    }

    /**
     * Commit a reservation to a sale — stock is physically depleted.
     */
    fun commit(quantity: Int) {
        require(reservedQuantity >= quantity) {
            "Cannot commit more than reserved. Reserved: $reservedQuantity, Committing: $quantity"
        }
        totalQuantity -= quantity
        reservedQuantity -= quantity
        // availableQuantity does not change (was already deducted on reserve)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Inventory) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

// entity/InventoryReservation.kt
@Entity
@Table(name = "inventory_reservations")
class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    var warehouse: Warehouse? = null

    @Column(name = "order_id")
    var orderId: UUID? = null

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ReservationStatus = ReservationStatus.PENDING

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now().plusSeconds(900) // 15 minutes

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InventoryReservation) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

enum class ReservationStatus { PENDING, COMMITTED, RELEASED, EXPIRED }
```

### Inventory Repository

```kotlin
// repository/InventoryRepository.kt
package com.zepto.backend.repository

import com.zepto.backend.entity.Inventory
import com.zepto.backend.entity.InventoryReservation
import com.zepto.backend.entity.ReservationStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface InventoryRepository : JpaRepository<Inventory, UUID> {

    // PESSIMISTIC_WRITE = SELECT FOR UPDATE
    // Used when we need to modify inventory — prevents concurrent modifications
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId")
    fun findByProductAndWarehouseForUpdate(
        @Param("productId") productId: UUID,
        @Param("warehouseId") warehouseId: UUID
    ): Optional<Inventory>

    fun findByProductIdAndWarehouseId(productId: UUID, warehouseId: UUID): Optional<Inventory>
}

@Repository
interface InventoryReservationRepository : JpaRepository<InventoryReservation, UUID> {

    // Find expired PENDING reservations — called by scheduled cleanup job
    fun findByStatusAndExpiresAtBefore(
        status: ReservationStatus,
        cutoff: Instant
    ): List<InventoryReservation>

    // Bulk expire — UPDATE is more efficient than individual deletes
    @Modifying
    @Query("""
        UPDATE InventoryReservation r 
        SET r.status = 'EXPIRED', r.updatedAt = :now 
        WHERE r.status = 'PENDING' AND r.expiresAt < :cutoff
    """)
    fun expireStaleReservations(@Param("cutoff") cutoff: Instant, @Param("now") now: Instant): Int
}
```

### Reservation Expiry Cleanup Job

```kotlin
// service/InventoryCleanupScheduler.kt
package com.zepto.backend.service

import com.zepto.backend.entity.ReservationStatus
import com.zepto.backend.repository.InventoryRepository
import com.zepto.backend.repository.InventoryReservationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class InventoryCleanupScheduler(
    private val reservationRepository: InventoryReservationRepository,
    private val inventoryRepository: InventoryRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Runs every 60 seconds. Releases stock held by expired reservations.
     *
     * CRITICAL: This must be idempotent — if it runs twice concurrently (two instances),
     * processing the same reservation twice would release stock twice.
     * The status transition PENDING → EXPIRED is the idempotency guard.
     * The @Modifying query below atomically updates and returns affected count.
     */
    @Scheduled(fixedDelay = 60_000) // 60 seconds, starts after previous execution completes
    @Transactional
    fun releaseExpiredReservations() {
        val now = Instant.now()
        
        // Find expired reservations before changing their status
        val expired = reservationRepository.findByStatusAndExpiresAtBefore(
            ReservationStatus.PENDING, now
        )
        
        if (expired.isEmpty()) return
        
        log.info("Found ${expired.size} expired reservations to release")
        
        // Release stock for each expired reservation
        expired.forEach { reservation ->
            val inventory = inventoryRepository.findByProductAndWarehouseForUpdate(
                reservation.product!!.id!!,
                reservation.warehouse!!.id!!
            ).orElseThrow()
            
            inventory.release(reservation.quantity)
        }
        
        // Bulk update status
        val updatedCount = reservationRepository.expireStaleReservations(now, now)
        log.info("Released $updatedCount expired reservations")
    }
}
```

---

## 2.4 Order & Order Item Schema

### Order State Machine

Every order goes through a lifecycle. The state machine defines which transitions are **valid** — attempting an invalid transition (e.g., delivering a cancelled order) is a business logic error that must be rejected at the application level, not silently accepted.

```
PLACED → CONFIRMED → PACKED → OUT_FOR_DELIVERY → DELIVERED
   │          │          │
   └──────────┴──────────┴──→ CANCELLED
   
REFUND_INITIATED can come from CANCELLED or DELIVERED (for return)
```

### `V4__create_orders_tables.sql`

```sql
-- V4__create_orders_tables.sql

-- Order payment status
CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PAID',
    'FAILED',
    'REFUNDED',
    'PARTIALLY_REFUNDED'
);

-- Order fulfillment status
CREATE TYPE order_status AS ENUM (
    'PLACED',
    'CONFIRMED',
    'PACKED',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED',
    'REFUND_INITIATED',
    'REFUNDED'
);

CREATE TABLE delivery_addresses (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    label           VARCHAR(50) NOT NULL DEFAULT 'Home',  -- "Home", "Work", "Other"
    full_name       VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    address_line_1  TEXT        NOT NULL,
    address_line_2  TEXT,
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    pincode         VARCHAR(10) NOT NULL,
    landmark        VARCHAR(255),
    latitude        DECIMAL(9, 6),
    longitude       DECIMAL(9, 6),
    is_default      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT delivery_addresses_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_delivery_addresses_user ON delivery_addresses (user_id);

-- ── Orders ─────────────────────────────────────────────────────────────────────
CREATE TABLE orders (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    warehouse_id        UUID            NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    
    -- Denormalized delivery address snapshot
    -- Why snapshot? The user may delete their address, but the order address must persist.
    -- Foreign key to delivery_addresses table is also kept for analytics JOIN purposes.
    delivery_address_id UUID            REFERENCES delivery_addresses(id) ON DELETE SET NULL,
    delivery_address_snapshot JSONB     NOT NULL,  -- Full address at time of order
    
    -- Status fields
    status              order_status    NOT NULL DEFAULT 'PLACED',
    payment_status      payment_status  NOT NULL DEFAULT 'PENDING',
    
    -- Financials — denormalized for fast retrieval (don't re-sum order_items every time)
    subtotal            DECIMAL(12, 2)  NOT NULL,
    delivery_fee        DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    total_amount        DECIMAL(12, 2)  NOT NULL,
    
    -- Payment details
    payment_method      VARCHAR(50),   -- "UPI", "CARD", "COD", "WALLET"
    payment_gateway_id  VARCHAR(255),  -- Razorpay order_id or payment_id
    
    -- Delivery
    estimated_delivery_at   TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     TEXT,
    
    -- Optimistic locking — critical for concurrent status updates
    -- Example: delivery partner marks delivered while admin marks cancelled
    -- Without @Version, last write wins. With @Version, second writer gets conflict exception.
    version             BIGINT          NOT NULL DEFAULT 0,
    
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT orders_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_orders_user_id ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders (status, created_at DESC);
CREATE INDEX idx_orders_warehouse ON orders (warehouse_id, status);
CREATE INDEX idx_orders_payment_status ON orders (payment_status) WHERE payment_status = 'PENDING';

-- Constraint: delivered_at only set when status = DELIVERED
ALTER TABLE orders ADD CONSTRAINT check_delivered_at 
    CHECK (delivered_at IS NULL OR status IN ('DELIVERED', 'REFUND_INITIATED', 'REFUNDED'));

CREATE TRIGGER orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── Order Items ────────────────────────────────────────────────────────────────
CREATE TABLE order_items (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    order_id        UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id      UUID            NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    
    -- Snapshot fields — never JOIN to products for historical order data
    -- Product name/price may change; order should reflect what was purchased
    product_name    VARCHAR(255)    NOT NULL,
    product_image_url TEXT,
    
    quantity        INT             NOT NULL CHECK (quantity > 0),
    
    -- Price at time of purchase — immutable after order placed
    unit_price      DECIMAL(12, 2)  NOT NULL,
    discount_amount DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    total_price     DECIMAL(12, 2)  NOT NULL,
    
    -- GST/Tax breakdown
    tax_percent     DECIMAL(5, 2)   NOT NULL DEFAULT 0,
    tax_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0,
    
    CONSTRAINT order_items_pkey PRIMARY KEY (id),
    CONSTRAINT check_total_price CHECK (total_price = (unit_price - discount_amount) * quantity)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
```

### Order Entity with State Machine Enforcement

```kotlin
// entity/Order.kt
package com.zepto.backend.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener::class)
class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null

    @Column(name = "warehouse_id", nullable = false)
    var warehouseId: UUID? = null

    @Column(name = "delivery_address_id")
    var deliveryAddressId: UUID? = null

    // JSONB column for address snapshot
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address_snapshot", columnDefinition = "jsonb", nullable = false)
    var deliveryAddressSnapshot: Map<String, Any?> = emptyMap()

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = OrderStatus.PLACED

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    var subtotal: BigDecimal = BigDecimal.ZERO

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    var deliveryFee: BigDecimal = BigDecimal.ZERO

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    var taxAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "payment_method", length = 50)
    var paymentMethod: String? = null

    @Column(name = "payment_gateway_id", length = 255)
    var paymentGatewayId: String? = null

    @Column(name = "estimated_delivery_at")
    var estimatedDeliveryAt: Instant? = null

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null

    @Column(name = "cancellation_reason")
    var cancellationReason: String? = null

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<OrderItem> = mutableListOf()

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null

    /**
     * State machine enforcement at the entity level.
     * This runs in the service layer BEFORE persisting.
     * The DB does not enforce state transitions — the application does.
     */
    fun transitionTo(newStatus: OrderStatus) {
        val validTransitions = mapOf(
            OrderStatus.PLACED to setOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED to setOf(OrderStatus.PACKED, OrderStatus.CANCELLED),
            OrderStatus.PACKED to setOf(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED),
            OrderStatus.OUT_FOR_DELIVERY to setOf(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED to setOf(OrderStatus.REFUND_INITIATED),
            OrderStatus.CANCELLED to emptySet(),
            OrderStatus.REFUND_INITIATED to setOf(OrderStatus.REFUNDED),
            OrderStatus.REFUNDED to emptySet()
        )

        val allowed = validTransitions[status] ?: emptySet()
        if (newStatus !in allowed) {
            throw IllegalStateException(
                "Invalid order status transition: $status → $newStatus. Allowed: $allowed"
            )
        }

        status = newStatus
        when (newStatus) {
            OrderStatus.DELIVERED -> deliveredAt = Instant.now()
            OrderStatus.CANCELLED -> cancelledAt = Instant.now()
            else -> {}
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Order) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

enum class OrderStatus {
    PLACED, CONFIRMED, PACKED, OUT_FOR_DELIVERY, DELIVERED,
    CANCELLED, REFUND_INITIATED, REFUNDED
}

enum class PaymentStatus {
    PENDING, PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED
}

@Entity
@Table(name = "order_items")
class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null

    @Column(name = "product_id", nullable = false)
    var productId: UUID? = null

    // Snapshot fields — never lazy-load product just to show order history
    @Column(name = "product_name", nullable = false, length = 255)
    var productName: String = ""

    @Column(name = "product_image_url")
    var productImageUrl: String? = null

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    var unitPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    var totalPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "tax_percent", nullable = false, precision = 5, scale = 2)
    var taxPercent: BigDecimal = BigDecimal.ZERO

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    var taxAmount: BigDecimal = BigDecimal.ZERO

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderItem) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
```

### Order Repository

```kotlin
// repository/OrderRepository.kt
package com.zepto.backend.repository

import com.zepto.backend.entity.Order
import com.zepto.backend.entity.OrderStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {

    fun findByUserId(userId: UUID, pageable: Pageable): Page<Order>

    fun findByUserIdAndStatus(userId: UUID, status: OrderStatus, pageable: Pageable): Page<Order>

    // Optimistic lock read — loads with version for concurrent update detection
    @Lock(LockModeType.OPTIMISTIC)
    fun findWithLockById(id: UUID): Optional<Order>

    // PESSIMISTIC lock for critical status transitions (payment processing)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): Optional<Order>

    // Dashboard query: orders grouped by status
    @Query("""
        SELECT o.status AS status, COUNT(o) AS count 
        FROM Order o 
        WHERE o.warehouseId = :warehouseId 
        AND o.createdAt >= :since
        GROUP BY o.status
    """)
    fun countByStatusForWarehouse(
        @Param("warehouseId") warehouseId: UUID,
        @Param("since") since: java.time.Instant
    ): List<OrderStatusCount>
}

interface OrderStatusCount {
    val status: OrderStatus
    val count: Long
}
```

---

## 2.5 Wallet & Ledger Schema

### Why You Cannot Use FLOAT for Money

```
// Java/Kotlin REPL demonstration of floating point money problems:
val a = 0.1 + 0.2
// a = 0.30000000000000004  ← NOT 0.3

val price = 99.99
val tax = price * 0.18
// tax = 17.998200000000003  ← NOT 17.9982
```

This is IEEE 754 binary floating point. There is no exact binary representation for most decimal fractions. At Zepto scale (millions of transactions/day), rounding errors accumulate into material financial discrepancies that regulators will notice. Always use:
- **Database**: `DECIMAL(19, 4)` — 19 total digits, 4 decimal places
- **Application**: `java.math.BigDecimal` — arbitrary precision decimal
- **Never**: `FLOAT`, `DOUBLE`, `Double`, `Float`

> [!CAUTION]
> If you store wallet balances as `DOUBLE PRECISION` in PostgreSQL, you will have reconciliation failures. The SUM of all ledger entries will not exactly equal the stored balance because each floating point addition accumulates rounding error. This has caused real fintech companies to discover millions of rupees in phantom discrepancies during audits.

### `V5__create_wallet_tables.sql`

```sql
-- V5__create_wallet_tables.sql

CREATE TYPE ledger_type AS ENUM (
    'CREDIT',   -- Money coming in
    'DEBIT'     -- Money going out
);

CREATE TYPE ledger_reference_type AS ENUM (
    'ORDER_PAYMENT',        -- Wallet used to pay for order
    'ORDER_REFUND',         -- Refund credited to wallet
    'PROMOTIONAL_CREDIT',   -- Promo/referral credit
    'WITHDRAWAL',           -- Withdrawal to bank
    'DEPOSIT',              -- Bank transfer in
    'ADJUSTMENT'            -- Manual adjustment by admin
);

CREATE TABLE wallets (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    
    -- DECIMAL(19, 4): supports balances up to 999,999,999,999,999.9999
    -- 4 decimal places to handle paise-level precision
    -- PostgreSQL NUMERIC is arbitrary precision — DECIMAL is an alias
    balance         DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    
    -- Frozen amount: money that's reserved for pending transactions
    -- balance - frozen_amount = spendable amount
    frozen_amount   DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    
    version         BIGINT          NOT NULL DEFAULT 0,  -- Optimistic locking
    
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    
    CONSTRAINT wallets_pkey PRIMARY KEY (id),
    CONSTRAINT wallets_user_id_unique UNIQUE (user_id),  -- One wallet per user
    CONSTRAINT check_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT check_frozen_non_negative CHECK (frozen_amount >= 0),
    CONSTRAINT check_frozen_lte_balance CHECK (frozen_amount <= balance)
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);

CREATE TRIGGER wallets_updated_at
    BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── Wallet Ledger — Immutable Double-Entry Bookkeeping ─────────────────────────
CREATE TABLE wallet_ledger (
    id                  UUID                    NOT NULL DEFAULT gen_random_uuid(),
    wallet_id           UUID                    NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    
    type                ledger_type             NOT NULL,
    amount              DECIMAL(19, 4)          NOT NULL CHECK (amount > 0),
    
    -- Balance snapshot after this transaction — for quick balance verification
    balance_after       DECIMAL(19, 4)          NOT NULL CHECK (balance_after >= 0),
    
    -- What caused this entry
    reference_type      ledger_reference_type   NOT NULL,
    reference_id        UUID                    NOT NULL,  -- order_id, withdrawal_id, etc.
    
    description         TEXT                    NOT NULL,
    
    -- Idempotency key — prevents double-crediting/debiting if request is retried
    idempotency_key     VARCHAR(255),
    
    -- Metadata for audit
    created_by          UUID                    REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    
    -- Ledger entries are NEVER updated or deleted
    CONSTRAINT wallet_ledger_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_ledger_wallet_id ON wallet_ledger (wallet_id, created_at DESC);
CREATE INDEX idx_ledger_reference ON wallet_ledger (reference_type, reference_id);
CREATE UNIQUE INDEX idx_ledger_idempotency ON wallet_ledger (idempotency_key) 
    WHERE idempotency_key IS NOT NULL;

-- ── Reconciliation View ────────────────────────────────────────────────────────
-- This view lets you verify wallet balance integrity on demand
-- Run: SELECT * FROM wallet_reconciliation WHERE discrepancy != 0
CREATE VIEW wallet_reconciliation AS
SELECT 
    w.id AS wallet_id,
    w.user_id,
    w.balance AS stored_balance,
    COALESCE(
        SUM(CASE WHEN l.type = 'CREDIT' THEN l.amount ELSE 0 END) -
        SUM(CASE WHEN l.type = 'DEBIT'  THEN l.amount ELSE 0 END),
        0
    ) AS computed_balance,
    w.balance - COALESCE(
        SUM(CASE WHEN l.type = 'CREDIT' THEN l.amount ELSE 0 END) -
        SUM(CASE WHEN l.type = 'DEBIT'  THEN l.amount ELSE 0 END),
        0
    ) AS discrepancy
FROM wallets w
LEFT JOIN wallet_ledger l ON l.wallet_id = w.id
GROUP BY w.id, w.user_id, w.balance;
```

### Wallet Entity and Service

```kotlin
// entity/Wallet.kt
package com.zepto.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "wallets")
class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "user_id", nullable = false, unique = true)
    var userId: UUID? = null

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    var balance: BigDecimal = BigDecimal.ZERO.setScale(4)

    @Column(name = "frozen_amount", nullable = false, precision = 19, scale = 4)
    var frozenAmount: BigDecimal = BigDecimal.ZERO.setScale(4)

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    /**
     * Available balance = balance - frozen
     * This is how much the user can actually spend right now.
     */
    val spendableBalance: BigDecimal
        get() = balance.subtract(frozenAmount)

    fun credit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "Credit amount must be positive" }
        balance = balance.add(amount)
    }

    fun debit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO) { "Debit amount must be positive" }
        require(amount <= spendableBalance) {
            "Insufficient balance. Available: $spendableBalance, Requested: $amount"
        }
        balance = balance.subtract(amount)
    }

    fun freeze(amount: BigDecimal) {
        require(amount <= spendableBalance) { "Cannot freeze more than spendable balance" }
        frozenAmount = frozenAmount.add(amount)
    }

    fun unfreeze(amount: BigDecimal) {
        require(amount <= frozenAmount) { "Cannot unfreeze more than frozen amount" }
        frozenAmount = frozenAmount.subtract(amount)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Wallet) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

// entity/WalletLedger.kt
@Entity
@Table(name = "wallet_ledger")
class WalletLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    var wallet: Wallet? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: LedgerType? = null

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    var balanceAfter: BigDecimal = BigDecimal.ZERO

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    var referenceType: LedgerReferenceType? = null

    @Column(name = "reference_id", nullable = false)
    var referenceId: UUID? = null

    @Column(name = "description", nullable = false)
    var description: String = ""

    @Column(name = "idempotency_key", length = 255)
    var idempotencyKey: String? = null

    @Column(name = "created_by")
    var createdBy: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WalletLedger) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

enum class LedgerType { CREDIT, DEBIT }
enum class LedgerReferenceType {
    ORDER_PAYMENT, ORDER_REFUND, PROMOTIONAL_CREDIT, WITHDRAWAL, DEPOSIT, ADJUSTMENT
}
```

### Wallet Service — Production-Grade Financial Operations

```kotlin
// service/WalletService.kt
package com.zepto.backend.service

import com.zepto.backend.entity.*
import com.zepto.backend.exception.ConflictException
import com.zepto.backend.exception.ResourceNotFoundException
import com.zepto.backend.exception.ValidationException
import com.zepto.backend.repository.WalletLedgerRepository
import com.zepto.backend.repository.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val ledgerRepository: WalletLedgerRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Debit wallet for order payment.
     *
     * Isolation.SERIALIZABLE: Highest isolation level.
     * Ensures that no other transaction can read or write to this wallet
     * between our READ (check balance) and WRITE (deduct).
     *
     * Without SERIALIZABLE: Two concurrent payments could both read
     * balance=500, both approve, both deduct 400 → balance goes to -300. 
     * (Lost update problem with READ_COMMITTED)
     *
     * @Retryable: SERIALIZABLE transactions can be rolled back due to
     * serialization failures. Retry up to 3 times with exponential backoff.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100, multiplier = 2.0)
    )
    fun debitForOrder(
        userId: UUID,
        orderId: UUID,
        amount: BigDecimal,
        idempotencyKey: String
    ) {
        // Check idempotency first — prevent double-debit on retry
        if (ledgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Duplicate debit request with idempotency key: $idempotencyKey")
            return
        }

        val wallet = walletRepository.findByUserIdWithLock(userId)
            ?: throw ResourceNotFoundException("Wallet", userId)

        if (!wallet.isActive) {
            throw ValidationException("Wallet is suspended", "WALLET_SUSPENDED")
        }

        if (wallet.spendableBalance < amount) {
            throw ValidationException(
                "Insufficient wallet balance. Available: ${wallet.spendableBalance}, Required: $amount",
                "INSUFFICIENT_BALANCE"
            )
        }

        wallet.debit(amount)

        val entry = WalletLedger().apply {
            this.wallet = wallet
            type = LedgerType.DEBIT
            this.amount = amount
            balanceAfter = wallet.balance
            referenceType = LedgerReferenceType.ORDER_PAYMENT
            referenceId = orderId
            description = "Order payment: $orderId"
            this.idempotencyKey = idempotencyKey
        }

        walletRepository.save(wallet)
        ledgerRepository.save(entry)

        log.info("Wallet debited: userId=$userId, amount=$amount, orderId=$orderId, newBalance=${wallet.balance}")
    }

    /**
     * Credit wallet for refund.
     * Uses REPEATABLE_READ — lighter than SERIALIZABLE.
     * Credits don't have the same lost-update risk (adding money doesn't deplete a resource).
     * But we still need REPEATABLE_READ to prevent phantom reads of the balance snapshot.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun creditRefund(
        userId: UUID,
        orderId: UUID,
        amount: BigDecimal,
        idempotencyKey: String
    ) {
        if (ledgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Duplicate credit request with idempotency key: $idempotencyKey")
            return
        }

        val wallet = walletRepository.findByUserIdWithLock(userId)
            ?: throw ResourceNotFoundException("Wallet", userId)

        wallet.credit(amount)

        val entry = WalletLedger().apply {
            this.wallet = wallet
            type = LedgerType.CREDIT
            this.amount = amount
            balanceAfter = wallet.balance
            referenceType = LedgerReferenceType.ORDER_REFUND
            referenceId = orderId
            description = "Refund for order: $orderId"
            this.idempotencyKey = idempotencyKey
        }

        walletRepository.save(wallet)
        ledgerRepository.save(entry)

        log.info("Wallet credited: userId=$userId, amount=$amount, orderId=$orderId")
    }

    /**
     * Reconciliation: verify stored balance matches ledger sum.
     * Run this as a scheduled job or expose as an admin endpoint.
     */
    @Transactional(readOnly = true)
    fun reconcile(walletId: UUID): ReconciliationResult {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { ResourceNotFoundException("Wallet", walletId) }

        val computedBalance = ledgerRepository.computeBalance(walletId)

        val discrepancy = wallet.balance.subtract(computedBalance)
        val isBalanced = discrepancy.compareTo(BigDecimal.ZERO) == 0

        if (!isBalanced) {
            log.error("WALLET RECONCILIATION FAILURE: walletId=$walletId, stored=${wallet.balance}, computed=$computedBalance, discrepancy=$discrepancy")
        }

        return ReconciliationResult(
            walletId = walletId,
            storedBalance = wallet.balance,
            computedBalance = computedBalance,
            discrepancy = discrepancy,
            isBalanced = isBalanced
        )
    }
}

data class ReconciliationResult(
    val walletId: UUID,
    val storedBalance: BigDecimal,
    val computedBalance: BigDecimal,
    val discrepancy: BigDecimal,
    val isBalanced: Boolean
)
```

```kotlin
// repository/WalletRepository.kt
package com.zepto.backend.repository

import com.zepto.backend.entity.Wallet
import com.zepto.backend.entity.WalletLedger
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface WalletRepository : JpaRepository<Wallet, UUID> {

    fun findByUserId(userId: UUID): Wallet?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    fun findByUserIdWithLock(@Param("userId") userId: UUID): Wallet?
}

@Repository
interface WalletLedgerRepository : JpaRepository<WalletLedger, UUID> {

    fun existsByIdempotencyKey(key: String): Boolean

    // Reconciliation query: SUM(CREDIT) - SUM(DEBIT) = computed balance
    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN l.type = 'CREDIT' THEN l.amount ELSE -l.amount END),
            0
        )
        FROM WalletLedger l WHERE l.wallet.id = :walletId
    """)
    fun computeBalance(@Param("walletId") walletId: UUID): BigDecimal
}
```

---

## 2.6 Transactions, Locking, Concurrency

### Isolation Levels — When to Use What

PostgreSQL's default isolation level is `READ COMMITTED`. Spring Boot inherits this default. Here's exactly when to override it:

```kotlin
// The isolation level matrix for our system:

// READ_COMMITTED (default): 
// Use for: Most read operations, single-entity updates where optimistic locking is sufficient
// Risk: Non-repeatable reads (same row read twice in same transaction may give different values)
@Transactional(isolation = Isolation.READ_COMMITTED)
fun getProductDetails(id: UUID): ProductResponse { ... }

// REPEATABLE_READ:
// Use for: Inventory reservation (read qty, check, write) within one transaction
// Guarantees: The same row read twice in the same transaction gives the same value
// Risk: Phantom reads (new rows appearing between reads in same transaction)
// Prevents: Lost updates on individual rows
@Transactional(isolation = Isolation.REPEATABLE_READ)
fun reserveInventory(productId: UUID, qty: Int): InventoryReservation { ... }

// SERIALIZABLE:
// Use for: Wallet debits, financial calculations involving aggregates
// Guarantees: Full transaction isolation — as if transactions ran one at a time
// Risk: Higher lock contention, serialization failures → must implement retry
// Prevents: All anomalies including phantoms and serialization anomalies
@Transactional(isolation = Isolation.SERIALIZABLE)
fun debitWallet(userId: UUID, amount: BigDecimal): Unit { ... }
```

> [!WARNING]
> `SERIALIZABLE` in PostgreSQL uses Serializable Snapshot Isolation (SSI), not traditional locking. SSI is highly concurrent but can throw `PSQLException: ERROR: could not serialize access due to read/write dependencies among transactions`. Your code MUST handle this with retry logic. Without retry, a serialization failure looks like a 500 to the user.

### Pessimistic Locking — `SELECT FOR UPDATE`

```kotlin
// service/InventoryService.kt — Pessimistic locking for inventory reservation

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: InventoryReservationRepository,
    private val movementRepository: InventoryMovementRepository
) {

    /**
     * Reserve inventory during checkout.
     *
     * PESSIMISTIC_WRITE = SELECT FOR UPDATE
     * This acquires an exclusive row lock immediately.
     * All other transactions trying to reserve the SAME product+warehouse
     * will BLOCK until this transaction commits or rolls back.
     *
     * Why pessimistic here instead of optimistic?
     * - Inventory is a hot-spot row — multiple users simultaneously trying
     *   to buy the last unit of a product.
     * - Optimistic locking here means: load row, check qty, reserve,
     *   commit → but if 10 users all load the same row concurrently, 
     *   9 of them will get OptimisticLockException and need to retry.
     * - Pessimistic locking serializes these 10 users at the DB level.
     * - At Zepto scale (flash sales): pessimistic is more predictable.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    fun reserveStock(
        productId: UUID,
        warehouseId: UUID,
        userId: UUID,
        quantity: Int
    ): InventoryReservation {
        // Acquires FOR UPDATE lock — other transactions block here until we commit
        val inventory = inventoryRepository.findByProductAndWarehouseForUpdate(productId, warehouseId)
            .orElseThrow { ResourceNotFoundException("Inventory", "$productId@$warehouseId") }

        // Business validation — inside the lock, so the check is consistent
        if (inventory.availableQuantity < quantity) {
            throw ValidationException(
                "Insufficient stock. Available: ${inventory.availableQuantity}, Requested: $quantity",
                "INSUFFICIENT_STOCK"
            )
        }

        // Mutate entity — Hibernate will flush UPDATE at transaction commit
        inventory.reserve(quantity)

        val reservation = InventoryReservation().apply {
            this.product = inventory.product
            this.warehouse = inventory.warehouse
            this.userId = userId
            this.quantity = quantity
            // Lock expires in 15 minutes — cleanup job handles expiry
            this.expiresAt = java.time.Instant.now().plusSeconds(900)
        }

        // Record movement for audit trail
        val movement = buildMovement(inventory, MovementType.RESERVATION, quantity, null, userId)

        reservationRepository.save(reservation)
        inventoryRepository.save(inventory)
        movementRepository.save(movement)

        return reservation
    }

    private fun buildMovement(
        inventory: Inventory,
        type: MovementType,
        quantity: Int,
        referenceId: UUID?,
        createdBy: UUID?
    ): InventoryMovement {
        return InventoryMovement().apply {
            this.inventory = inventory
            this.productId = inventory.product!!.id!!
            this.warehouseId = inventory.warehouse!!.id!!
            this.type = type
            this.quantityDelta = if (type == MovementType.RESERVATION) -quantity else quantity
            // snapshot before/after for audit
        }
    }
}
```

### Optimistic Locking — `@Version` and Retry

```kotlin
// service/OrderService.kt — Optimistic locking for order status transitions

@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Update order status — uses optimistic locking via @Version.
     *
     * Scenario: Delivery partner's app and admin dashboard both try to update
     * the same order concurrently.
     * - Thread A loads order (version=5), wants to set DELIVERED
     * - Thread B loads order (version=5), wants to set CANCELLED
     * - Thread A commits first: UPDATE orders SET status='DELIVERED', version=6 WHERE id=? AND version=5
     *   → 1 row updated ✅
     * - Thread B tries: UPDATE orders SET status='CANCELLED', version=6 WHERE id=? AND version=5
     *   → 0 rows updated (version already 6) → Spring throws ObjectOptimisticLockingFailureException
     *
     * This prevents the "last write wins" problem without heavyweight locks.
     */
    @Transactional
    fun updateStatus(orderId: UUID, newStatus: OrderStatus): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order", orderId) }

        // State machine validates the transition — throws IllegalStateException if invalid
        order.transitionTo(newStatus)

        // Hibernate issues: UPDATE orders SET status=?, version=? WHERE id=? AND version=?
        // If another transaction changed the version between our load and this save,
        // ObjectOptimisticLockingFailureException is thrown
        return orderRepository.save(order).toResponse()
    }
}
```

```kotlin
// Exception handler for optimistic locking failures in GlobalExceptionHandler.kt

@ExceptionHandler(ObjectOptimisticLockingFailureException::class)
fun handleOptimisticLockingFailure(
    ex: ObjectOptimisticLockingFailureException,
    request: HttpServletRequest
): ResponseEntity<ApiError> {
    log.warn("Optimistic locking conflict at ${request.requestURI}: ${ex.message}")

    val error = ApiError(
        code = "CONCURRENT_MODIFICATION",
        message = "The resource was modified by another request. Please retry your operation.",
        path = request.requestURI,
        correlationId = MDC.get(MDC_CORRELATION_KEY)
    )

    // 409 Conflict — client should re-fetch and retry
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
}
```

### `SELECT FOR UPDATE SKIP LOCKED` — Job Queue Pattern

```sql
-- Job queue pattern: multiple worker instances pick up jobs without contention
-- SKIP LOCKED means: skip rows already locked by another worker (don't block)
-- This is how Sidekiq, GoodJob, and PostgreSQL-backed queues work

SELECT * FROM orders 
WHERE status = 'PLACED' 
AND created_at < NOW() - INTERVAL '5 minutes'  -- only pick up unprocessed orders
ORDER BY created_at ASC
LIMIT 10
FOR UPDATE SKIP LOCKED;
```

```kotlin
// repository/OrderRepository.kt — SKIP LOCKED for background processing

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {

    // Native query because JPQL doesn't support SKIP LOCKED
    @Query(value = """
        SELECT * FROM orders 
        WHERE status = 'PLACED' 
        AND created_at < NOW() - INTERVAL '5 minutes'
        ORDER BY created_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    fun findUnprocessedOrdersForUpdate(@Param("batchSize") batchSize: Int = 50): List<Order>
}

// service/OrderProcessingJob.kt
@Component
class OrderProcessingJob(private val orderRepository: OrderRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 30_000)  // Every 30 seconds
    @Transactional
    fun processOrders() {
        // Each pod in K8s picks up different rows — no overlap, no duplicate processing
        val orders = orderRepository.findUnprocessedOrdersForUpdate(50)
        
        if (orders.isEmpty()) return
        
        log.info("Processing ${orders.size} unprocessed orders")
        orders.forEach { order ->
            try {
                // Confirm order: validate payment, update status
                order.transitionTo(OrderStatus.CONFIRMED)
                // ... trigger warehouse notification
            } catch (e: Exception) {
                log.error("Failed to process order ${order.id}", e)
                // Don't rethrow — we want to process other orders
                // Dead letter handling: move to failed state after N attempts
            }
        }
    }
}
```

> [!NOTE]
> `FOR UPDATE SKIP LOCKED` is the foundation of PostgreSQL-backed job queues. It eliminates the need for a separate Redis/SQS queue for many use cases. Stripe uses a similar pattern internally for their idempotency key processing. For quick-commerce order confirmation, this is simpler and more reliable than Kafka for low-to-medium volume operations.

### HikariCP — Production Pool Configuration Explained

```yaml
# application-prod.yml — annotated explanation
spring:
  datasource:
    hikari:
      # Total connections to maintain to PostgreSQL.
      # Rule: (number_of_cpu_cores * 2) + effective_spindle_count
      # For RDS db.r5.large (2 vCPU, SSD): max = (2*2) + 1 = 5... but in practice
      # web apps have I/O wait, so multiply by 2-3: 10-15 connections per instance
      # If you have 5 instances: 5 * 15 = 75 connections to PostgreSQL
      # PostgreSQL default max_connections = 100. Plan accordingly.
      maximum-pool-size: 20
      
      # Minimum connections to keep alive even during low traffic
      # Prevents cold-start latency when traffic ramps up
      minimum-idle: 5
      
      # How long to wait for a connection from the pool before throwing exception
      # 30 seconds is too long for a user-facing API — fail fast at 3-5 seconds
      # Don't set this too low or you'll get false failures during transient DB slowness
      connection-timeout: 5000  # 5 seconds for prod
      
      # How long a connection can sit idle before being closed
      # Set to less than your DB's wait_timeout to prevent stale connections
      idle-timeout: 600000  # 10 minutes
      
      # Maximum lifetime of a connection — rotate before DB closes it
      # RDS default wait_timeout = 8 hours. Set max-lifetime < that.
      max-lifetime: 1800000  # 30 minutes
      
      # Prevents firewall/NAT from killing "idle" connections
      # Sends TCP keepalive or a ping query
      keepalive-time: 60000  # 1 minute
      
      # Logs stack trace of where connection was acquired if held too long
      # This is how you find connection leaks BEFORE they cause an outage
      leak-detection-threshold: 60000  # 1 minute
      
      # Pool name appears in JMX metrics, thread names, and logs
      pool-name: ZeptoProd
```

### Deadlock Prevention Strategy

```kotlin
// service/MultiResourceService.kt — Always acquire locks in consistent order

/**
 * WRONG: Potential deadlock
 * Transaction A: locks Inventory A, then tries to lock Inventory B
 * Transaction B: locks Inventory B, then tries to lock Inventory A
 * → Circular wait → deadlock → one transaction is killed by PostgreSQL
 */
fun wrong_processMultiItemOrder(items: List<OrderItem>) {
    items.forEach { item ->  // Items may be in different order for different transactions
        val inventory = inventoryRepository.findByProductAndWarehouseForUpdate(
            item.productId, item.warehouseId
        )
        // ... process
    }
}

/**
 * CORRECT: Always sort lock acquisition order consistently
 * Transaction A: sorts items → locks ID "aaa", then ID "bbb"
 * Transaction B: sorts items → locks ID "aaa" (blocks), then ID "bbb"
 * → No circular wait → no deadlock
 */
@Transactional
fun correct_processMultiItemOrder(items: List<OrderItem>) {
    // Sort by product_id + warehouse_id to ensure consistent lock ordering
    // Every transaction that locks multiple inventory rows uses this same order
    val sortedItems = items.sortedWith(
        compareBy<OrderItem> { it.productId.toString() }
            .thenBy { it.warehouseId.toString() }
    )
    
    sortedItems.forEach { item ->
        val inventory = inventoryRepository.findByProductAndWarehouseForUpdate(
            item.productId, item.warehouseId
        )
        inventory.reserve(item.quantity)
        inventoryRepository.save(inventory)
    }
}
```

> [!CAUTION]
> PostgreSQL detects deadlocks and kills one of the transactions (throws `PSQLException: ERROR: deadlock detected`). This surfaces as a 500 error to the user. At Blinkit/Zepto scale, deadlocks during flash sales can cause order placement failures. The consistent lock ordering pattern eliminates this class of bug entirely — it doesn't require any special PostgreSQL configuration.

### Full Transaction Annotation Reference

```kotlin
// Transaction annotation cheat sheet for production Spring Boot

@Transactional
// → Uses default propagation (REQUIRED), default isolation (READ_COMMITTED)
// → Rolls back on RuntimeException and Error only
// → Does NOT roll back on checked exceptions

@Transactional(readOnly = true)
// → Hibernate skips dirty checking, enables read-replica routing
// → Cannot execute INSERT/UPDATE/DELETE (throws exception)
// → Use as class-level default, override methods that write

@Transactional(isolation = Isolation.REPEATABLE_READ)
// → Prevents non-repeatable reads — same row returns same data within transaction
// → Use for: inventory reservations, balance-sensitive reads

@Transactional(isolation = Isolation.SERIALIZABLE)
// → Strongest — prevents all anomalies including phantoms
// → Use for: wallet debits, financial aggregates
// → MUST pair with @Retryable for serialization failures

@Transactional(propagation = Propagation.REQUIRES_NEW)
// → Always creates a NEW transaction, suspending the current one
// → Use for: audit logging (must commit even if outer transaction rolls back)
// → Use for: sending events/notifications that should persist independently

@Transactional(propagation = Propagation.NOT_SUPPORTED)
// → Executes without a transaction, suspending any current transaction
// → Use for: operations that are inherently non-transactional (Redis calls)

@Transactional(timeout = 30)
// → Transaction automatically rolls back after 30 seconds
// → Prevents long-running transactions from holding locks indefinitely
// → Critical for: background jobs that process user data

@Transactional(rollbackFor = [IOException::class])
// → Also rolls back for checked exceptions (default only rolls back on RuntimeException)
```

---

## Production Database Readiness Checklist

| Category | Item | Status |
|----------|------|--------|
| Schema | UUID primary keys everywhere | ✅ |
| Schema | TIMESTAMPTZ (not TIMESTAMP) for all timestamps | ✅ |
| Schema | DECIMAL(19,4) for all monetary amounts | ✅ |
| Schema | `updated_at` trigger on all mutable tables | ✅ |
| Schema | Soft delete pattern on users | ✅ |
| Schema | `@Version` on all concurrently-modified entities | ✅ |
| Migrations | Flyway manages all DDL — Hibernate DDL disabled | ✅ |
| Migrations | `validate-on-migrate: true` | ✅ |
| Migrations | All migrations immutable after apply | ✅ |
| Indexes | Unique partial indexes on email/phone (soft delete aware) | ✅ |
| Indexes | Composite status+time index for dashboard queries | ✅ |
| Indexes | `expires_at` index on reservations for cleanup | ✅ |
| Indexes | GIN index for product full-text search | ✅ |
| Inventory | Reservation system (not naive stock decrement) | ✅ |
| Inventory | Immutable movement audit log | ✅ |
| Inventory | Expiry cleanup scheduled job | ✅ |
| Orders | State machine with valid transitions enforced | ✅ |
| Orders | Price snapshot in order_items (not joined to products) | ✅ |
| Orders | Address snapshot in JSONB (not only FK) | ✅ |
| Wallet | Ledger-based balance (not just a balance column) | ✅ |
| Wallet | Reconciliation query to verify integrity | ✅ |
| Wallet | Idempotency key on ledger entries | ✅ |
| Concurrency | Pessimistic locking on inventory | ✅ |
| Concurrency | Optimistic locking on orders | ✅ |
| Concurrency | SERIALIZABLE + retry on wallet | ✅ |
| Concurrency | Consistent lock ordering to prevent deadlocks | ✅ |
| Concurrency | SKIP LOCKED for job queue pattern | ✅ |
| Pool | HikariCP max-pool-size tuned for instance count | ✅ |
| Pool | `leak-detection-threshold` configured | ✅ |
| Pool | `connection-timeout` fail-fast (< 5s for user-facing APIs) | ✅ |
## Book-Aligned Database Corrections

The book's database chapters make an important distinction:

- `DataSource` + HikariCP is the connection infrastructure.
- JDBC/JdbcTemplate is direct SQL access.
- Spring Data JDBC gives repository style without JPA's full ORM behavior.
- Spring Data JPA uses Hibernate and entity lifecycle/persistence context.
- Spring Data REST can expose repositories, but that is usually not how to build serious domain APIs.

For your delivery/Blinkit backend:

```text
JPA:
  users, addresses, orders, products, stores

JDBC/Spring Data JDBC:
  ledger entries, append-only audit tables, simple aggregate persistence

Redis:
  active cart, cache, counters, short-lived state
```

Do not treat JPA as the only database tool. Choose by aggregate complexity and performance risk.
