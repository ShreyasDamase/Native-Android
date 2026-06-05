# MP-3 — Food Ordering and Delivery System

Build a Swiggy/Zomato-style backend at mini-project scale.

This project connects many backend concepts into one complete app: users, restaurants, menu catalog, cart, checkout, payment sandbox, order lifecycle and delivery assignment.

Full reference codebase:

[code/food-ordering](/Users/shreyasdamase/Sentinel/Native-Android-Notes/Backend%20Development/Springboot/Mini%20Projects/code/food-ordering)

## Learning Goals

- Build a modular monolith with multiple domain modules.
- Model order state machines.
- Separate catalog, cart, checkout and order responsibilities.
- Use Redis for temporary cart state.
- Use events for notifications and delivery assignment.
- Protect checkout with idempotency.

## Core Features

### Phase 1: Identity and Restaurant Catalog

Build:

- User registration/login.
- Restaurant listing.
- Menu categories.
- Menu items.
- Restaurant availability.

APIs:

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
GET /api/v1/restaurants
GET /api/v1/restaurants/{restaurantId}/menu
```

### Phase 2: Cart

APIs:

```http
GET /api/v1/cart
POST /api/v1/cart/items
PATCH /api/v1/cart/items/{itemId}
DELETE /api/v1/cart/items/{itemId}
```

Rules:

- One cart belongs to one user.
- One cart can contain items from only one restaurant.
- Cart price must be recalculated from server-side menu prices.
- Do not trust client-side price.

### Phase 3: Checkout

APIs:

```http
POST /api/v1/checkout/quote
POST /api/v1/orders
```

Rules:

- Validate restaurant is open.
- Validate menu items are active.
- Calculate taxes, delivery fee and platform fee.
- Use `Idempotency-Key` for order creation.
- Payment is sandbox only.

### Phase 4: Order Lifecycle

Order states:

```text
CREATED
PAYMENT_PENDING
PAYMENT_CONFIRMED
ACCEPTED_BY_RESTAURANT
PREPARING
READY_FOR_PICKUP
PICKED_UP
DELIVERED
CANCELLED
```

APIs:

```http
GET /api/v1/orders/{orderId}
PATCH /api/v1/orders/{orderId}/status
GET /api/v1/users/me/orders
```

### Phase 5: Async Events

Events:

```text
OrderCreated
PaymentConfirmed
RestaurantAcceptedOrder
DeliveryAssignmentRequested
OrderDelivered
```

Start with in-process Spring events. Later replace with RabbitMQ or Kafka.

## Domain Modules

```text
identity
restaurant
menu
cart
checkout
order
payment
delivery
notification
```

## Database Tables

Start with:

```text
users
restaurants
restaurant_hours
menu_categories
menu_items
orders
order_items
payments
delivery_assignments
idempotency_keys
```

Optional Redis keys:

```text
cart:{userId}
checkout_quote:{quoteId}
rate_limit:{userId}
```

## Package Structure

```text
food/
  common/
  identity/
  restaurant/
  menu/
  cart/
  checkout/
  order/
  payment/
  delivery/
  notification/
```

Inside each module:

```text
api/
application/
domain/
infrastructure/
```

## Design Lessons

Important boundaries:

- Catalog tells what can be ordered.
- Cart stores what the user intends to buy.
- Checkout validates current reality.
- Order stores the committed transaction.
- Payment records money authorization in sandbox mode.
- Delivery tracks assignment and fulfillment.

Never create the final order only from cart data without revalidating restaurant, menu item, quantity and price.

## Tests

Required tests:

- User can register and login.
- Restaurant menu can be listed.
- Cart rejects items from a second restaurant.
- Checkout quote recalculates server-side price.
- Order creation is idempotent.
- Invalid order state transition fails.
- Payment confirmation moves order to the next valid state.
- Order history returns only current user's orders.

## Stretch Features

- Restaurant owner role.
- Delivery partner role.
- Redis cart TTL.
- RabbitMQ notification worker.
- Elasticsearch restaurant search.
- Prometheus metrics for order state counts.
