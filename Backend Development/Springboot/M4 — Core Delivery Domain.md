# M4 — Core Delivery Domain

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

This is where most backends fail. Order state machines, inventory reservation & checkout flows are the hardest part.

The book correction: build this as a modular Spring Boot system first. Do not jump directly to microservices. Use Spring Modulith-style thinking: each module exposes only the API/events it wants other modules to use.

## Implementation Order

4.1 Build Product Catalog API:

- Products
- Variants
- Categories
- Brands
- Search-ready structure

4.2 Build Store / Dark Store module:

- Store location
- Service radius
- Store status
- Capacity

4.3 Build Inventory module:

- Stock updates
- Store-level availability
- Reservation
- Release

4.4 Build Address module:

- User addresses
- Geolocation
- Delivery zones

4.5 Build Cart + Checkout flow:

- Validate inventory
- Validate store serviceability
- Recalculate prices
- Reserve inventory only when checkout starts

4.6 Build Order state machine:

```text
PLACED -> CONFIRMED -> PACKED -> OUT_FOR_DELIVERY -> DELIVERED
       -> CANCELLED
```

4.7 Build Delivery Partner assignment and tracking.

## Boundary Rule

Controllers should not coordinate the domain. Put workflow coordination in services/application use cases, and emit events for async follow-up work.
