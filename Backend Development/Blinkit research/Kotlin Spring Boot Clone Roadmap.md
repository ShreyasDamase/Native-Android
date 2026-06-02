# Kotlin Spring Boot Clone Roadmap

Goal: rebuild the important backend ideas from Blinkit as a production-grade learning project in Kotlin + Spring Boot.

The project should live under backend development because this is primarily a system design and backend architecture study, not an Android UI project.

Related notes:

- [[00 Blinkit Research Index]]
- [[Quick Commerce Domain Research]]
- [[Kotlin Spring Boot Backend Design]]
- [[Blinkit Data Model Draft]]

## Phase 1: Foundation

Build:

- API Gateway
- Auth Service
- Catalog Service

Core features:

- Phone/OTP-style auth in sandbox mode
- JWT/session handling
- Product/category APIs
- Product detail APIs
- Basic homepage feed

Spring stack:

- Kotlin
- Spring Boot
- Spring Web
- Spring Security
- PostgreSQL
- Flyway

## Phase 2: Location-Aware Catalog

Build:

- Address Service
- Serviceability Service
- Store/dark-store model

Core features:

- Saved addresses
- Lat/lng location model
- Store service zones
- Product availability by store
- ETA placeholder logic

Important lesson:

Blinkit is not just a catalog. It is a location-aware catalog where product availability and delivery promise depend on the selected address.

## Phase 3: Cart

Build:

- Cart Service
- Cart item mutation APIs
- Cart validation

API shape inspired by observed endpoints:

```http
POST /carts
PUT /carts/{cartId}
PATCH /carts/{cartId}
POST /carts/{cartId}/share
```

Core features:

- Create cart
- Add item
- Update quantity
- Remove item
- Validate serviceability
- Validate inventory
- Recalculate price

Storage:

- Redis for active cart
- PostgreSQL for persistent cart snapshots

## Phase 4: Inventory / Fulfillment

Build:

- Inventory Service
- ETA Service
- Store availability model

Core features:

- Inventory per store
- Stock reservation
- ETA calculation
- No-delivery-partner/serviceability responses

Events:

```text
InventoryReserved
InventoryReleased
InventoryAdjusted
DeliveryEtaCalculated
```

## Phase 5: Checkout and Payment Sandbox

Build payment only with sandbox/test providers.

Core features:

- Checkout quote
- Delivery fee calculation
- Payment method listing
- Payment intent creation in test mode
- Never store raw card data

Do not implement real money movement until the architecture is mature.

API shape:

```http
POST /checkout/quote
GET /payment-methods
POST /payments/intents
POST /payments/webhook
```

## Phase 6: Orders

Build:

- Order Service
- Order status state machine

Core features:

- Create order after payment authorization
- Reserve inventory
- Track order states
- Order history
- Order count

Order states:

```text
CREATED
PAYMENT_PENDING
PAYMENT_AUTHORIZED
INVENTORY_RESERVED
PICKING
PACKED
OUT_FOR_DELIVERY
DELIVERED
CANCELLED
```

## Phase 7: Event-Driven Architecture

Add Kafka.

Events:

```text
CartUpdated
CheckoutStarted
PaymentAuthorized
OrderCreated
InventoryReserved
DeliveryAssignmentRequested
OrderDelivered
```

Rules:

- User-facing cart validation stays synchronous.
- Analytics, notifications, inventory reconciliation, and delivery workflows can be async.

## Phase 8: Production Hardening

Add:

- Docker Compose
- Kubernetes manifests
- Centralized config
- Observability
- Rate limiting
- Idempotency keys
- Distributed tracing
- Circuit breakers
- Retry with backoff
- Dead-letter topics

## Suggested Repository Layout

```text
blinkit-clone/
  api-gateway/
  auth-service/
  catalog-service/
  address-service/
  cart-service/
  inventory-service/
  order-service/
  payment-service/
  common/
  docker-compose.yml
```

## First Implementation Milestone

Build these first:

1. `catalog-service`
2. `address-service`
3. `cart-service`

Reason:

This combination captures the most important Blinkit backend lesson: cart behavior depends on catalog, inventory, and address/serviceability, not just product IDs and quantities.

## Better MVP Sequence

Use this as the actual build order:

1. Monorepo setup with Gradle Kotlin DSL
2. Common module with IDs, errors, DTOs, auth context
3. PostgreSQL + Flyway baseline
4. Catalog Service
5. Store Service
6. Inventory Service
7. Address Service
8. Cart Service
9. Checkout Service
10. Payment Service in sandbox/mock mode
11. Order Service
12. Kafka events
13. Redis caching
14. API Gateway

Do not start with payment. Start with catalog + store inventory + cart validation. That is the heart of quick commerce.
