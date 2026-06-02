# Kotlin Spring Boot Backend Design

This is the practical backend design direction for the Blinkit-style project.

## Target Architecture

```text
Client
  |
  v
API Gateway
  |
  +-- Auth Service
  +-- Address Service
  +-- Catalog Service
  +-- Search Service
  +-- Store Service
  +-- Inventory Service
  +-- Cart Service
  +-- Checkout Service
  +-- Payment Service
  +-- Order Service
  +-- Fulfillment Service
  +-- Notification Service
  +-- Analytics Service
```

## Recommended Spring Stack

Use:

- Kotlin
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Redis
- Kafka
- Resilience4j
- Micrometer
- OpenTelemetry
- Docker Compose

Avoid at first:

- Kubernetes too early
- Too many services before the domain works
- Complex CQRS/event sourcing before basic flows are correct

## Service Responsibilities

### Auth Service

Handles:

- Phone login
- OTP verification in sandbox mode
- JWT/session token
- User profile

Core APIs:

```http
POST /auth/otp/request
POST /auth/otp/verify
GET /users/me
```

### Address Service

Handles:

- Saved addresses
- Lat/lng coordinates
- Address labels
- Serviceability lookup

Core APIs:

```http
GET /addresses
POST /addresses
PUT /addresses/{addressId}
GET /locations/autocomplete
GET /locations/serviceability?lat=&lng=
```

### Store Service

Handles:

- Dark stores
- Store service radius
- Store open/closed status
- Store capacity

Core APIs:

```http
GET /stores/nearest?lat=&lng=
GET /stores/{storeId}
PUT /stores/{storeId}/status
```

### Catalog Service

Handles:

- Products
- Categories
- Brands
- Product images
- SEO-like metadata

Core APIs:

```http
GET /categories
GET /products/{productId}
GET /products?categoryId=&storeId=
POST /catalog/listing
```

Important: listing should accept `storeId` or location context, because availability differs by store.

### Inventory Service

Handles:

- Store-level stock
- Reservations
- Stock release
- Low-stock alerts

Core APIs:

```http
GET /inventory/stores/{storeId}/products/{productId}
POST /inventory/reservations
DELETE /inventory/reservations/{reservationId}
```

### Cart Service

Handles:

- Active cart
- Cart items
- Cart recalculation
- Serviceability checks
- Inventory checks

Core APIs:

```http
POST /carts
GET /carts/current
PATCH /carts/{cartId}/items/{productId}
DELETE /carts/{cartId}/items/{productId}
POST /carts/{cartId}/validate
```

Cart validation should call:

- Catalog Service
- Inventory Service
- Address/Store Service
- Pricing/Promotion Service

### Checkout Service

Handles:

- Checkout quote
- Delivery fee
- Taxes
- Coupon validation
- Inventory reservation request
- Payment intent request

Core APIs:

```http
POST /checkout/quote
POST /checkout/start
POST /checkout/{checkoutId}/confirm
```

### Payment Service

Use sandbox only.

Handles:

- Payment methods
- Payment intent
- Webhook
- Payment status

Core APIs:

```http
GET /payments/methods
POST /payments/intents
POST /payments/webhook
GET /payments/{paymentId}
```

### Order Service

Handles:

- Order creation
- Order state machine
- Order history
- Order status timeline

Core APIs:

```http
POST /orders
GET /orders
GET /orders/{orderId}
GET /orders/{orderId}/timeline
```

## Important Backend Patterns

### Idempotency

Required for:

- Add to cart
- Checkout start
- Payment intent
- Order creation
- Webhook handling

Use header:

```http
Idempotency-Key: uuid
```

Store idempotency records:

```text
idempotency_keys
  key
  user_id
  request_hash
  response_body
  status
  expires_at
```

### Cart Recalculation

Every cart mutation should recalculate:

- Product price
- Quantity validity
- Store availability
- Delivery fee
- ETA
- Promotions
- Taxes
- Minimum order constraints

### Inventory Reservation

Do not reserve inventory on every cart add.

Better approach:

```text
Cart add -> soft availability check
Checkout start -> reserve inventory for short TTL
Payment success -> convert reservation into order allocation
Payment failure/timeout -> release reservation
```

### Event Flow

```text
CartUpdated
CheckoutStarted
InventoryReserved
PaymentAuthorized
OrderCreated
PickerTaskCreated
DeliveryAssignmentRequested
OrderDelivered
```

Use Kafka for:

- Order lifecycle events
- Notifications
- Analytics
- Inventory reconciliation
- Delivery assignment

Do not use Kafka as an excuse to avoid synchronous correctness in checkout.

### Caching

Use Redis for:

- Homepage feed
- Category listings
- Product detail
- Feature flags
- Session/current cart
- Serviceability result with short TTL

Do not cache blindly:

- Payment state
- Inventory reservation state
- Order state transitions

## First Working MVP

Build one monorepo with modules:

```text
blinkit-backend/
  api-gateway/
  auth-service/
  catalog-service/
  address-service/
  store-service/
  inventory-service/
  cart-service/
  checkout-service/
  order-service/
  common/
```

MVP flow:

```text
Login
  -> Select address
  -> Find nearest store
  -> Browse available products
  -> Add item to cart
  -> Validate cart
  -> Start checkout
  -> Reserve inventory
  -> Simulate payment
  -> Create order
```

