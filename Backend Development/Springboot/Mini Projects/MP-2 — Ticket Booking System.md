# MP-2 — Ticket Booking System

Build a backend for booking seats for movies, buses or events.

This project teaches the backend problem every booking app must solve: many users want the same limited seat at the same time.

Full reference codebase:

[code/ticket-booking](/Users/shreyasdamase/Sentinel/Native-Android-Notes/Backend%20Development/Springboot/Mini%20Projects/code/ticket-booking)

## Learning Goals

- Design availability and reservation models.
- Handle temporary holds.
- Prevent double booking.
- Use transactions and expiration.
- Add idempotency to checkout.
- Separate search/read APIs from booking/write APIs.

## Core Features

### Phase 1: Catalog

Build:

- Venue.
- Screen or event hall.
- Event or show.
- Seat layout.
- Seat category and price.

APIs:

```http
GET /api/v1/events
GET /api/v1/events/{eventId}
GET /api/v1/events/{eventId}/seats
```

### Phase 2: Seat Hold

API:

```http
POST /api/v1/seat-holds
```

Request:

```json
{
  "eventId": "uuid",
  "seatIds": ["uuid-1", "uuid-2"]
}
```

Rules:

- A hold expires after 5 minutes.
- A seat can have only one active hold.
- A booked seat cannot be held.
- The response returns `holdId`, `expiresAt` and price summary.

### Phase 3: Booking Checkout

API:

```http
POST /api/v1/bookings
```

Headers:

```http
Idempotency-Key: client-generated-key
```

Rules:

- Booking requires a valid active hold.
- Booking marks seats as booked.
- Booking creates payment record in sandbox mode.
- Repeating the same idempotency key returns the same booking result.

### Phase 4: Expiry Job

Build a scheduled job:

```text
ReleaseExpiredSeatHoldsJob
```

Rules:

- Expired holds should no longer block seat availability.
- The job must be safe to run multiple times.

## Domain Model

```text
Venue
Hall
Seat
Event
SeatPrice
SeatHold
SeatHoldItem
Booking
BookingItem
BookingPayment
```

States:

```text
SeatHoldStatus = ACTIVE, EXPIRED, CONFIRMED, CANCELLED
BookingStatus = CREATED, PAYMENT_PENDING, CONFIRMED, CANCELLED
PaymentStatus = PENDING, SUCCESS, FAILED
```

## Database Tables

Start with:

```text
venues
halls
seats
events
seat_prices
seat_holds
seat_hold_items
bookings
booking_items
booking_payments
idempotency_keys
```

Important constraints:

- A seat cannot be booked twice for the same event.
- An active hold should uniquely protect a seat for an event.
- A booking must reference a confirmed hold.
- Idempotency key must be unique per user and operation.

## Package Structure

```text
booking/
  api/
    EventController.kt
    SeatHoldController.kt
    BookingController.kt
  application/
    CreateSeatHoldUseCase.kt
    ConfirmBookingUseCase.kt
    ReleaseExpiredSeatHoldsJob.kt
  domain/
    SeatHold.kt
    Booking.kt
    SeatAvailabilityPolicy.kt
    BookingPriceCalculator.kt
  infrastructure/
    EventJpaRepository.kt
    SeatHoldJpaRepository.kt
    BookingJpaRepository.kt
```

## Concurrency Lesson

The critical race:

```text
User A and User B try to hold seat S1 at the same time.
Only one should succeed.
```

For PostgreSQL, learn:

- Unique constraints.
- Row-level locks.
- Transaction isolation.
- Retrying on constraint violation when needed.

## Tests

Required tests:

- Seat map shows available, held and booked seats.
- Hold cannot include already booked seat.
- Hold expires after configured time.
- Booking succeeds with active hold.
- Booking fails with expired hold.
- Same idempotency key does not create duplicate bookings.
- Concurrent hold requests cannot hold the same seat twice.

## Stretch Features

- Dynamic pricing.
- Coupon codes.
- Waitlist.
- Refund flow.
- Kafka event: `BookingConfirmed`.
- Email/SMS notification sandbox.
