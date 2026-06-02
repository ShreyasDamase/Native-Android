# Quick Commerce Domain Research

## Why Blinkit Is Architecturally Interesting

Blinkit is a quick-commerce platform. Unlike normal ecommerce, the main constraint is not just product discovery or checkout. The main constraint is speed under local inventory and delivery capacity.

In public investor material, Eternal/Zomato describes Blinkit as part of its quick-commerce business and reports rapid store expansion. In Q2 FY26, Eternal reported 1,816 quick-commerce stores and said the business had moved mostly to an own-inventory model. It also said quick-commerce capex was heavily concentrated in store/warehouse capacity. Source: [Eternal Q2FY26 shareholder letter PDF](https://b.zmtcdn.com/investor-relations/Eternal_Shareholders_Letter_Q2FY26_Results.pdf).

Eternal's Q4 FY26 update says 109 million Indians transacted across Blinkit, District, and Zomato in FY26, with B2C NOV of INR 26,880 crore in Q4FY26. Source: [Eternal Q4FY26 update](https://www.eternal.com/blog/q4fy26/).

Public reporting also describes Blinkit's quick-commerce model as dark-store-driven and location-first. Source: [The Week on Blinkit and dark stores](https://theweek.com/business/economy/blinkit-indias-10-minute-delivery-app).

## Marketplace vs Inventory-Led Model

Earlier quick-commerce marketplace model:

```text
Seller owns inventory
Platform lists products
Dark store / seller side handles stock
Platform earns commission/take rate
```

Inventory-led model:

```text
Blinkit owns most inventory
Blinkit controls buying, stocking, pricing, availability
Revenue is closer to full order value
Working capital and supply-chain complexity increase
Margins can improve if execution is strong
```

Eternal's Q2FY26 letter said about 80% of quick-commerce NOV was already on own inventory, expected to move toward about 90% steady state. This matters for backend design because inventory ownership means the platform must run serious systems for procurement, replenishment, expiry, stock accuracy, pricing, shrinkage, GST/invoicing, and store-level accounting.

## Dark Store Model

A dark store is a small fulfillment warehouse serving a tight geographic radius.

Backend implication:

- Every user location maps to one or more eligible stores.
- Catalog availability is store-specific.
- Inventory is local, not global.
- ETA depends on store distance, picker workload, delivery-partner availability, and traffic.
- Cart validity can change if the address changes.

Simplified flow:

```text
User Location
  -> Serviceability Engine
  -> Candidate Dark Stores
  -> Inventory Availability
  -> Catalog Filtering
  -> ETA Promise
  -> Cart Validation
```

## What Makes Quick Commerce Hard

Normal ecommerce can show a product and ship it later.

Blinkit-style quick commerce must answer these questions immediately:

- Is the item available in the nearest dark store?
- Can the store pick and pack this order quickly?
- Is there a delivery partner available?
- Will the ETA still be true after adding more items?
- Can inventory be reserved during checkout?
- What happens if stock disappears between cart and payment?
- How do substitutions, refunds, cancellations, and failed payments work?

## Backend Systems Blinkit Probably Needs

### Customer-Facing Systems

- Auth Service
- Address Service
- Catalog Service
- Search Service
- Recommendation/Layout Service
- Cart Service
- Checkout Service
- Payment Service
- Order Service
- Delivery Tracking Service

### Operations Systems

- Store Management Service
- Inventory Service
- Procurement Service
- Replenishment Service
- Picking/Packing Service
- Delivery Assignment Service
- Pricing/Promotion Service
- Refund Service
- Customer Support Service

### Platform Systems

- API Gateway
- Feature Flag Service
- Analytics/Event Pipeline
- Notification Service
- Experimentation Service
- Observability
- Fraud/Risk Service

## What To Copy For Learning

For a Kotlin + Spring Boot learning project, copy the architecture ideas, not the exact product:

- Store-specific inventory
- Location-based serviceability
- Cart recalculation
- Checkout quote
- Payment sandbox
- Order state machine
- Kafka events
- Redis caching
- PostgreSQL schema
- API gateway routing
- Idempotency and retries

## What Not To Copy

Do not try to copy:

- Real payment flows
- Real Blinkit private APIs
- Real customer data
- Real logistics algorithms
- Real anti-fraud rules
- Production marketplace/inventory legal setup

The learning target is backend architecture, not unauthorized access or commercial replication.

## Sources

- [Eternal Q2FY26 shareholder letter PDF](https://b.zmtcdn.com/investor-relations/Eternal_Shareholders_Letter_Q2FY26_Results.pdf)
- [Eternal Q4FY26 shareholder update](https://www.eternal.com/blog/q4fy26/)
- [The Week: Blinkit, dark stores, and 10-minute delivery](https://theweek.com/business/economy/blinkit-indias-10-minute-delivery-app)
- [arXiv: Dark store facility location optimization for quick commerce](https://arxiv.org/abs/2312.11494)

