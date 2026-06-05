# Food Ordering and Delivery System

Standalone Kotlin + Spring Boot reference mini project.

## Run

```bash
docker compose up -d
gradle bootRun
```

Swagger:

```text
http://localhost:8083/swagger-ui.html
```

## API Flow

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
GET /api/v1/restaurants
GET /api/v1/restaurants/{restaurantId}/menu
POST /api/v1/cart/items
POST /api/v1/checkout/quote
POST /api/v1/orders
PATCH /api/v1/orders/{orderId}/status
```

## What To Study

- `FoodService.createOrder`: validates current cart/menu/restaurant state before creating an order.
- `FoodOrder.moveTo`: order lifecycle state machine.
- `SecurityConfig`: minimal permissive security shell for learning; replace demo token with JWT later.
- `V1__init.sql`: source-of-truth schema and seed restaurant/menu.
