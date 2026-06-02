# M8 — Scale — Kafka, Kubernetes, Multi-region

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

The book correction: messaging is broader than Kafka, and Kubernetes is not the first scaling step. Understand AMQP/RabbitMQ, WebSockets/STOMP, RSocket, Docker Compose, Gateway, and Actuator before jumping to multi-region.

## Implementation Order

8.1 Choose messaging technology by use case:

- RabbitMQ/AMQP for work queues, routing, command-style async processing
- Kafka for durable event streams, analytics, replay, high-throughput event pipelines
- WebSocket/STOMP for browser/mobile realtime updates
- RSocket for bidirectional service communication when needed

8.2 Implement order, inventory, and notification events.

8.3 Implement read replica strategy for PostgreSQL.

8.4 Add Spring Cloud Gateway or NGINX/Ingress depending on routing need.

8.5 Kubernetes deployment:

- Pods
- Services
- ConfigMaps
- Secrets
- HPA
- Ingress

8.6 Design multi-region and disaster recovery strategy.

8.7 Add blue/green deployment and zero-downtime release strategy.

## Scaling Rule

First make the modular monolith correct and observable. Then split services. Then scale infrastructure.
