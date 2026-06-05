# Parking Lot Management System

Standalone Kotlin + Spring Boot reference mini project.

## Run

```bash
docker compose up -d
gradle bootRun
```

Swagger:

```text
http://localhost:8081/swagger-ui.html
```

## API Flow

```http
POST /api/v1/parking/tickets
POST /api/v1/parking/tickets/{ticketId}/quote
POST /api/v1/parking/tickets/{ticketId}/checkout
GET /api/v1/parking/tickets/active
GET /api/v1/parking/spots/available
```

## What To Study

- `ParkingService.createTicket`: transaction boundary and pessimistic spot locking.
- `ParkingSpot.occupy`: domain invariant.
- `ParkingFeeCalculator`: pure domain logic with unit tests.
- `V1__init.sql`: relational model, seed data and partial unique index.
