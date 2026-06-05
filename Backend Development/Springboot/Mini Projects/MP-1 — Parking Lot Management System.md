# MP-1 — Parking Lot Management System

Build a backend where vehicles enter a parking lot, get assigned a spot, pay a calculated fee, and exit safely.

This is the best first project because it looks simple but teaches real backend design: resource allocation, state transitions, pricing, transactions and race-condition prevention.

Full reference codebase:

[code/parking-lot](/Users/shreyasdamase/Sentinel/Native-Android-Notes/Backend%20Development/Springboot/Mini%20Projects/code/parking-lot)

## Learning Goals

- Model a domain with clear states.
- Design relational tables with constraints.
- Build REST APIs with validation.
- Prevent two vehicles from getting the same spot.
- Calculate parking fee based on time, vehicle type and spot type.
- Test transaction behavior.

## Core Features

### Phase 1: Basic Lot Setup

Build:

- Parking lot management.
- Floor management.
- Parking spot management.
- Vehicle type support.

Vehicle types:

```text
TWO_WHEELER
CAR
SUV
EV
```

Spot types:

```text
BIKE
COMPACT
LARGE
EV_CHARGING
ACCESSIBLE
```

### Phase 2: Entry Flow

API behavior:

```http
POST /api/v1/parking/tickets
```

Request:

```json
{
  "vehicleNumber": "MH12AB1234",
  "vehicleType": "CAR"
}
```

Response:

```json
{
  "ticketId": "uuid",
  "vehicleNumber": "MH12AB1234",
  "spotCode": "F1-C-014",
  "status": "ACTIVE",
  "entryTime": "2026-06-03T10:15:30Z"
}
```

Rules:

- A vehicle cannot have two active tickets.
- Assign the nearest available compatible spot.
- Mark the spot as occupied inside the same transaction.
- If no spot is available, return `409 Conflict`.

### Phase 3: Exit and Payment Flow

APIs:

```http
POST /api/v1/parking/tickets/{ticketId}/quote
POST /api/v1/parking/tickets/{ticketId}/checkout
```

Rules:

- Quote calculates fee but does not close the ticket.
- Checkout marks ticket as paid and frees the spot.
- Checkout must be idempotent. Repeating checkout should not double-charge.

### Phase 4: Admin and Reporting

APIs:

```http
GET /api/v1/parking/spots?status=AVAILABLE
GET /api/v1/parking/tickets/active
GET /api/v1/parking/reports/daily-revenue
```

## Domain Model

```text
ParkingLot
ParkingFloor
ParkingSpot
Vehicle
ParkingTicket
ParkingRate
ParkingPayment
```

Important states:

```text
SpotStatus = AVAILABLE, OCCUPIED, OUT_OF_SERVICE
TicketStatus = ACTIVE, PAYMENT_PENDING, PAID, CANCELLED
PaymentStatus = PENDING, SUCCESS, FAILED
```

## Database Tables

Start with:

```text
parking_lots
parking_floors
parking_spots
vehicles
parking_tickets
parking_rates
parking_payments
```

Important constraints:

- Unique active ticket per vehicle.
- Unique spot code per parking lot.
- Ticket must reference assigned spot.
- Payment must reference ticket.
- Spot status must be valid.

## Package Structure

```text
parking/
  api/
    ParkingTicketController.kt
    ParkingSpotController.kt
    ParkingReportController.kt
  application/
    CreateParkingTicketUseCase.kt
    CheckoutParkingTicketUseCase.kt
    CalculateParkingFeeUseCase.kt
  domain/
    ParkingTicket.kt
    ParkingSpot.kt
    ParkingRate.kt
    ParkingFeeCalculator.kt
  infrastructure/
    ParkingTicketJpaRepository.kt
    ParkingSpotJpaRepository.kt
```

## Concurrency Lesson

The main bug to prevent:

```text
Two entry requests arrive at the same time.
Both see the same available spot.
Both assign it.
```

Solve using one of these:

- Pessimistic row lock on available spots.
- Optimistic locking with `@Version`.
- Atomic update query that only occupies a spot if it is still available.

For learning, implement pessimistic locking first because it is easiest to reason about.

## Tests

Required tests:

- Fee calculation for first hour, extra hours and day cap.
- Vehicle type maps to compatible spot type.
- Creating ticket occupies a spot.
- Creating ticket fails when vehicle already has active ticket.
- Checkout frees the spot.
- Concurrent ticket creation does not assign the same spot twice.

## Stretch Features

- EV charging rate.
- Lost ticket penalty.
- Monthly pass.
- Admin JWT role.
- Spot maintenance mode.
- Prometheus metric for available spots.
