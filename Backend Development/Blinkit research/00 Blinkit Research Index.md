# Blinkit Research Index

This folder is for studying Blinkit as a backend engineering case study, then rebuilding the important ideas in Kotlin + Spring Boot.

## What I Am Researching

The goal is not to clone Blinkit's UI. The goal is to understand how a production quick-commerce backend is probably structured:

- Location-aware catalog
- Store/dark-store inventory
- Cart validation
- ETA and serviceability
- Checkout and payment orchestration
- Order lifecycle
- Event-driven fulfillment
- Analytics and experimentation
- Caching and high-read catalog traffic

## Current Notes

- [[Blinkit Backend Architecture Research]]
- [[Blinkit Endpoint Map]]
- [[Kotlin Spring Boot Clone Roadmap]]
- [[Quick Commerce Domain Research]]
- [[Kotlin Spring Boot Backend Design]]
- [[Blinkit Data Model Draft]]

## Research Done So Far

We manually opened `https://blinkit.com/` in Playwright Chromium and observed the web app network traffic.

Captured flows:

- Homepage/feed
- Location autocomplete
- Location resolution
- Login and OTP verification
- Address fetch
- Product listing
- Listing widgets
- Product detail
- Cart create/update/patch
- Cart share/assist
- Order count/history
- Profile/gift card/prescription pages
- Print vertical landing page
- ETA/serviceability calls

Not captured yet:

- Payment method listing
- Payment intent creation
- Checkout quote endpoint
- Coupon validation
- Order placement endpoint

Reason: we stopped before the payment flow to avoid accidentally placing or paying for an order.

## Best Mental Model

Blinkit is not just an ecommerce app.

It is a real-time, location-aware, inventory-constrained, logistics-heavy retail system.

The hard backend problem is this:

```text
Given a user location,
find the right dark store,
show only available products,
calculate realistic ETA,
keep cart valid as inventory changes,
reserve stock at checkout,
take payment safely,
create an order,
and coordinate picking + delivery quickly.
```

## Kotlin/Spring Boot Build Order

Build the backend in this order:

1. Catalog Service
2. Address / Location Service
3. Store + Inventory Service
4. Cart Service
5. Checkout Service
6. Payment Service in sandbox mode
7. Order Service
8. Delivery / Fulfillment Service
9. Kafka events
10. Redis caching
11. API Gateway
12. Docker Compose and Kubernetes

