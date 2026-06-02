# Blinkit Endpoint Map

Source: Manual Playwright request stream from Blinkit web exploration.

## Platform / Bootstrap

| Method | Endpoint | Purpose | Likely Service |
| --- | --- | --- | --- |
| GET | `/config/main` | Web runtime config | Platform Config |
| GET | `/visibility` | Visibility/session state | Platform Config |
| GET | `/api/feature-flags/receive` | Feature flags | Experimentation / Config |
| GET | `/v1/analytics_events_properties/` | Analytics metadata | Analytics |
| POST | `jumbo.blinkit.com/event` | Event tracking | Analytics |

## Auth / User

| Method | Endpoint | Purpose | Likely Service |
| --- | --- | --- | --- |
| GET | `/v2/accounts/auth_key/` | Fetch auth/session key | Auth Service |
| POST | `/v2/accounts/` | Start account/login flow | Auth Service |
| POST | `/v2/accounts/verify/phone/code/` | Verify phone OTP | Auth Service |

## Location / Address

| Method | Endpoint | Purpose | Likely Service |
| --- | --- | --- | --- |
| GET | `/location/autoSuggest` | Address autocomplete | Location Service |
| GET | `/location/info` | Resolve selected location | Location Service |
| GET | `/v4/address` | Fetch saved addresses | Address Service |

## Catalog / Search / Layout

| Method | Endpoint | Purpose | Likely Service |
| --- | --- | --- | --- |
| GET | `/feed/` | Homepage feed/layout | Catalog / Content Service |
| GET | `/v2/search/deeplink/` | Search deeplink metadata | Search Service |
| POST | `/v1/layout/listing` | Product/category listing | Catalog Service |
| POST | `/v1/layout/listing_widgets` | Listing widgets/recommendations | Catalog / Recommendation Service |
| POST | `/v1/layout/product/{productId}` | Product detail payload | Catalog Service |
| GET | `/seoInfo` | SEO metadata | Catalog Service |
| POST | `/v1/usecases` | Use-case/recipe suggestions | Recommendation Service |

## Cart / Checkout

| Method | Endpoint | Purpose | Likely Service |
| --- | --- | --- | --- |
| POST | `/v5/carts` | Create/fetch cart | Cart Service |
| PUT | `/v5/carts/{cartId}` | Replace/update cart | Cart Service |
| PATCH | `/v5/carts/{cartId}` | Partial cart mutation | Cart Service |
| POST | `/v1/assist/cart/share` | Share/assist cart action | Cart Service |

## Order / Profile

| Method | Endpoint | Purpose | Likely Service |
| --- | --- | --- | --- |
| GET | `/v1/order_count` | Fetch user order count | Order Service |
| POST | `/v1/layout/order_history` | Order history payload | Order Service |
| POST | `/v1/layout/prescriptions` | Prescriptions page payload | Health/Profile Service |
| POST | `/v1/layout/profile_egift_card` | Gift card profile payload | Wallet / Gift Card Service |

## Fulfillment / ETA

| Method | Endpoint | Purpose | Likely Service |
| --- | --- | --- | --- |
| GET | `/v1/consumerweb/eta` | Delivery ETA | Fulfillment Service |
| GET | `/v2/services/secondary-data` | Serviceability/secondary fulfillment data | Fulfillment Service |
| GET | `/v2/services/secondary-data/` | Serviceability/secondary fulfillment data | Fulfillment Service |

## Other Verticals

| Method | Endpoint | Purpose | Likely Service |
| --- | --- | --- | --- |
| GET | `/v1/print/landing_page` | Print service landing data | Print Vertical Service |

## Third Party / CDN

| Host | Purpose |
| --- | --- |
| `cdn.grofers.com` | Product images, category icons, layout assets, fonts |
| `maps.googleapis.com` | Address/map selection |
| `maps.gstatic.com` | Google Maps assets |
| `www.zomato.com/zpaykit/js/v5/hook.js` | Payment kit script loaded during bootstrap |
| `www.googletagmanager.com` | Tag manager |
| `www.google-analytics.com` | Analytics |
| `googleads.g.doubleclick.net` | Ads/conversion tracking |
| `connect.facebook.net`, `www.facebook.com` | Pixel/conversion tracking |
| `websdk.appsflyer.com` | Attribution/analytics |

