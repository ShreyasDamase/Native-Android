# Ticket Booking System

Standalone Kotlin + Spring Boot reference mini project.

## Run

```bash
docker compose up -d
gradle bootRun
```

Swagger:

```text
http://localhost:8082/swagger-ui.html
```

## API Flow

```http
GET /api/v1/events
GET /api/v1/events/{eventId}/seats
POST /api/v1/seat-holds
POST /api/v1/bookings
```

## What To Study

- `BookingService.createHold`: locks selected seats and marks them held.
- `BookingService.confirmBooking`: idempotent booking from active hold.
- `releaseExpiredHolds`: scheduled cleanup for expired holds.
- `Seat`: small domain object with legal state transitions.
