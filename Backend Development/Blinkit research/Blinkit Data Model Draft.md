# Blinkit Data Model Draft

This is a practical schema direction for the Kotlin + Spring Boot rebuild.

## Users

```sql
users
-----
id
phone_number
name
email
created_at
updated_at

otp_challenges
--------------
id
phone_number
otp_hash
expires_at
verified_at
attempt_count
created_at

sessions
--------
id
user_id
refresh_token_hash
expires_at
created_at
```

## Addresses

```sql
addresses
---------
id
user_id
label
line1
line2
city
state
pincode
latitude
longitude
is_default
created_at
updated_at
```

## Stores / Dark Stores

```sql
stores
------
id
name
code
latitude
longitude
status
service_radius_meters
opens_at
closes_at
created_at
updated_at

store_service_zones
-------------------
id
store_id
zone_name
polygon_geojson
active
```

## Catalog

```sql
categories
----------
id
name
parent_id
sort_order
active

brands
------
id
name

products
--------
id
name
brand_id
category_id
description
active
created_at
updated_at

product_variants
----------------
id
product_id
sku
unit_label
mrp
selling_price
tax_rate
active

product_images
--------------
id
product_id
url
sort_order
```

## Inventory

```sql
store_inventory
---------------
id
store_id
variant_id
available_quantity
reserved_quantity
low_stock_threshold
updated_at

inventory_reservations
----------------------
id
store_id
cart_id
order_id
variant_id
quantity
status
expires_at
created_at
```

Reservation statuses:

```text
ACTIVE
CONFIRMED
RELEASED
EXPIRED
```

## Cart

```sql
carts
-----
id
user_id
store_id
address_id
status
subtotal
delivery_fee
tax_amount
total
created_at
updated_at

cart_items
----------
id
cart_id
variant_id
quantity
unit_price
total_price
available
created_at
updated_at
```

Cart statuses:

```text
ACTIVE
CHECKOUT_STARTED
ORDERED
ABANDONED
```

## Checkout

```sql
checkouts
---------
id
cart_id
user_id
address_id
store_id
status
quote_total
expires_at
created_at
updated_at

checkout_price_lines
--------------------
id
checkout_id
line_type
label
amount
```

Checkout statuses:

```text
CREATED
INVENTORY_RESERVED
PAYMENT_PENDING
PAYMENT_AUTHORIZED
FAILED
EXPIRED
ORDER_CREATED
```

## Payments

```sql
payments
--------
id
checkout_id
user_id
provider
provider_payment_id
amount
currency
status
created_at
updated_at

payment_events
--------------
id
payment_id
event_type
payload_json
created_at
```

Payment statuses:

```text
CREATED
AUTHORIZED
CAPTURED
FAILED
REFUNDED
```

## Orders

```sql
orders
------
id
user_id
cart_id
checkout_id
payment_id
store_id
address_id
status
subtotal
delivery_fee
tax_amount
total
created_at
updated_at

order_items
-----------
id
order_id
variant_id
quantity
unit_price
total_price

order_status_events
-------------------
id
order_id
status
message
created_at
```

Order statuses:

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
REFUNDED
```

## Delivery

```sql
delivery_assignments
--------------------
id
order_id
partner_id
status
assigned_at
picked_up_at
delivered_at

delivery_partners
-----------------
id
name
phone_number
status
current_latitude
current_longitude
```

## Promotions

```sql
coupons
-------
id
code
discount_type
discount_value
min_order_value
starts_at
ends_at
active

cart_promotions
---------------
id
cart_id
coupon_id
discount_amount
```

## Analytics

For MVP, do not store every click in PostgreSQL.

Use Kafka topic:

```text
analytics.events
```

Event shape:

```json
{
  "eventId": "uuid",
  "userId": "uuid",
  "eventType": "PRODUCT_VIEWED",
  "occurredAt": "2026-06-03T00:00:00Z",
  "metadata": {}
}
```

