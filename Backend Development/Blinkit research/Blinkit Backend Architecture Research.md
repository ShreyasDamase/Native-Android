# Blinkit Backend Architecture Research

Date: 2026-06-03
Source: Manual Playwright network capture from `https://blinkit.com/`

Related notes:

- [[00 Blinkit Research Index]]
- [[Quick Commerce Domain Research]]
- [[Kotlin Spring Boot Backend Design]]
- [[Blinkit Data Model Draft]]
- [[Blinkit Endpoint Map]]

## Research Status

We opened Blinkit's web app in Playwright Chromium and manually exercised these flows:

- Homepage load
- Location/address search
- Login and OTP verification flow
- Product listing
- Product detail page
- Cart creation
- Cart item update and patch operations
- Address/map interaction
- Order history/profile areas
- Print vertical landing page

The first capture printed a rich terminal request stream but did not save the HAR because the process was stopped before Playwright closed its browser context. The recorder was then fixed to gracefully save HAR on Ctrl-C and continuously stream a request log to `network-requests.ndjson`.

Payment-specific endpoints were not reached. The safe next step is a focused capture that goes only up to the payment-options screen, stopping before any final pay/place-order action.

## Evidence Levels

Use this distinction while studying:

| Type | Meaning |
| --- | --- |
| Observed | We saw this endpoint or behavior in Playwright network traffic |
| Public research | Found in Eternal/Zomato updates or public reporting |
| Inferred | Reasonable backend design inference, not confirmed internal Blinkit architecture |

This note mixes all three, but the endpoint map is the strongest observed evidence.

## Public Research Context

Eternal's public updates show Blinkit is not only a marketplace-style app. The quick-commerce business has been moving toward an inventory-led model, which means the platform owns more of the stock and must manage procurement, warehousing, stock accuracy, pricing, and working capital more directly.

Backend implication:

```text
Marketplace model:
  seller inventory -> platform listing -> commission

Inventory-led model:
  platform-owned stock -> dark store inventory -> direct retail controls
```

For a Kotlin/Spring Boot clone, this means inventory should be a first-class service, not a small column on `products`.

## Main Architecture Signal

Blinkit's web architecture appears to be organized around a frontend layout engine plus domain APIs. Many product, cart, profile, and order pages are loaded through `/v1/layout/...` endpoints instead of simple REST resources. This suggests the backend sends page-specific structured payloads that the web client renders dynamically.

Likely high-level shape:

```text
Web Client
  |
  v
Blinkit Edge / API Gateway
  |
  +-- Config / Feature Flag Service
  +-- User / Auth Service
  +-- Location / Address Service
  +-- Catalog / Search Service
  +-- Recommendation / Layout Service
  +-- Cart Service
  +-- Order Service
  +-- Fulfillment / ETA Service
  +-- Wallet / Gift Card Service
  +-- Analytics Event Pipeline
  |
  +-- CDN for static assets and product images
  +-- Google Maps for address selection
```

## Better Backend Mental Model

Blinkit's core backend problem is:

```text
location + dark store + inventory + ETA + cart + payment + delivery
```

The catalog cannot be fully separated from location because a product is only useful if it is available near the user's selected address.

The cart cannot be a simple list of product IDs because every mutation may need to recalculate:

- Store availability
- Quantity limits
- ETA
- Delivery fees
- Promotions
- Taxes
- Minimum order rules
- Out-of-stock/substitution state

## Strongest Findings

1. Cart is a distinct backend boundary.

Observed:

- `POST /v5/carts`
- `PUT /v5/carts/{cartId}`
- `PATCH /v5/carts/{cartId}`
- `POST /v1/assist/cart/share`

This points to a cart service owning cart lifecycle, item quantity changes, cart sharing/assist actions, and probably price/serviceability recalculation.

2. Catalog is layout-driven.

Observed:

- `POST /v1/layout/listing`
- `POST /v1/layout/listing_widgets`
- `POST /v1/layout/product/{productId}`
- `GET /feed/`
- `GET /seoInfo`

The web client likely requests structured page layouts rather than raw product-only data. This allows Blinkit to mix products, widgets, ads, recommendations, use cases, and category blocks from one response.

3. Location and fulfillment are central.

Observed:

- `GET /location/autoSuggest`
- `GET /location/info`
- `GET /v4/address`
- `GET /v1/consumerweb/eta`
- `GET /v2/services/secondary-data`

Blinkit depends heavily on location/serviceability. Product availability, ETA, cart validity, and checkout eligibility probably depend on resolved location and nearby dark-store inventory.

4. User auth uses phone OTP.

Observed:

- `GET /v2/accounts/auth_key/`
- `POST /v2/accounts/`
- `POST /v2/accounts/verify/phone/code/`

This suggests a phone-first identity model, with session/auth keys fetched before account actions.

5. Analytics is pervasive.

Observed many calls to:

- `jumbo.blinkit.com/event`
- Google Analytics / Ads endpoints
- Facebook pixel endpoints
- AppsFlyer

Events are fired across homepage, listing, product detail, cart, account, and profile actions. In a production clone, analytics should be async and isolated from core business transactions.

## Inferred Service Boundaries

| Service | Responsibilities | Evidence |
| --- | --- | --- |
| API Gateway / Edge | Routing, auth/session forwarding, headers, rate limits | All public web APIs under `blinkit.com` |
| Config Service | Runtime config, visibility, feature flags | `/config/main`, `/visibility`, `/api/feature-flags/receive` |
| Auth Service | Phone login, OTP verification, session key | `/v2/accounts/...` |
| Address Service | Saved addresses, address lookup | `/v4/address`, `/location/...` |
| Catalog Service | Product listing, product details, SEO data | `/v1/layout/listing`, `/v1/layout/product/{id}`, `/seoInfo` |
| Search Service | Search deeplinks and likely query handling | `/v2/search/deeplink/` |
| Layout / Recommendation Service | Homepage feed, widgets, product recommendations, use cases | `/feed/`, `/v1/layout/listing_widgets`, `/v1/usecases` |
| Cart Service | Cart create/update/patch/share | `/v5/carts`, `/v1/assist/cart/share` |
| Fulfillment Service | ETA, serviceability, delivery partner/store availability | `/v1/consumerweb/eta`, `/v2/services/secondary-data` |
| Order Service | Order count/history | `/v1/order_count`, `/v1/layout/order_history` |
| Wallet / Gift Card Service | Gift card profile page, possibly wallet/payment prep | `/v1/layout/profile_egift_card` |
| Analytics Pipeline | User behavior/event collection | `jumbo.blinkit.com/event` |
| Print Vertical Service | Print feature landing/data | `/v1/print/landing_page` |

## Likely Data Model

### User / Auth

```text
users
user_phone_numbers
otp_challenges
sessions
```

### Address / Location

```text
addresses
geo_locations
serviceability_zones
dark_stores
store_service_areas
```

### Catalog

```text
products
product_variants
categories
brands
product_images
product_attributes
seo_metadata
```

### Inventory / Fulfillment

```text
stores
inventory_items
inventory_reservations
eta_rules
delivery_zones
delivery_partner_availability
```

### Cart

```text
carts
cart_items
cart_price_snapshots
cart_serviceability_checks
cart_promotions
```

### Order

```text
orders
order_items
order_status_events
order_payments
order_delivery_assignments
```

### Analytics

```text
event_stream
user_events
product_impressions
cart_events
checkout_events
```

## Caching Inference

Likely cached aggressively:

- Homepage feed/layout blocks
- Product images through CDN
- Category icons/assets
- Product listing widgets
- Search suggestions/deeplinks
- Feature flags/config

Likely not cached or cached very briefly:

- Cart state
- Address state
- ETA
- Serviceability
- Inventory availability
- Checkout/payment state

Probable pattern:

```text
Client
  -> API Gateway
  -> Redis / edge cache for public catalog blocks
  -> Catalog database/search index

Client
  -> API Gateway
  -> Cart Service
  -> Redis/session cart + persistent cart database
  -> Inventory/serviceability validation
```

## Event Architecture Inference

The cart and checkout path likely uses synchronous validation for user-facing correctness:

- Add item
- Recalculate cart
- Check location serviceability
- Check inventory availability
- Return updated cart

Order placement, once payment is authorized, likely emits events:

```text
OrderCreated
InventoryReserved
PaymentAuthorized
PickerTaskCreated
DeliveryAssignmentRequested
OrderPacked
OrderOutForDelivery
OrderDelivered
```

Likely async infrastructure:

- Kafka or similar event bus
- Async analytics ingestion
- Background inventory reconciliation
- Delivery assignment workflows
- Notification workers

## Kotlin + Spring Boot Rebuild Strategy

Build in this order:

1. API Gateway
2. Auth Service
3. Address / Location Service
4. Catalog Service
5. Cart Service
6. Inventory / Fulfillment Service
7. Order Service
8. Payment integration sandbox only
9. Event pipeline with Kafka
10. Redis caching
11. Docker Compose
12. Kubernetes deployment

## Immediate Next Capture

Goal: payment-options discovery without purchase.

Safe flow:

1. Start capture.
2. Login with OTP.
3. Set a serviceable address.
4. Add one low-cost item.
5. Open cart.
6. Continue only until payment options are visible.
7. Inspect UPI/card/wallet/COD options.
8. Stop capture before pressing final payment, placing order, or authorizing UPI/card.

Expected endpoints to discover:

- Checkout layout
- Cart validation
- Payment methods
- Wallet balance
- Payment intent/order quote
- Delivery fee calculation
- Coupon/promo validation
