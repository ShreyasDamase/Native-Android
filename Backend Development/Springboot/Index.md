# Spring Boot + Kotlin Backend Notes

## Existing App-Specific Track

These chapters use HireStory as the running example. Keep them as a concrete project walkthrough.

[[Chapter 1 — Spring Boot Fundamentals]]
[[Chapter 2 — JPA, Entities & Flyway]]
[[Chapter 3 — Repositories and Queries]]
[[Chapter 4 — REST Controllers, Validation & Error Handling]]
[[Chapter 5 — Spring Security & JWT]]
[[Chapter 6 — Redis - Caching, Counters & Speed]]
[[Chapter 7 — RabbitMQ-Async Processing & Notifications]]
[[Chapter 8 — The Web Crawler]]
[[Chapter 9 — Spring AI- Extracting Structured Data From Raw Text]]
[[Chapter 10 — Testing- Unit Tests, Integration Tests & Confidence]]
[[Chapter 11 — Deployment- Getting HireStory Live on Railway]]

## Generic Production Backend Track

Use these chapters when you want to build any serious backend: delivery app, Uber-style marketplace, hotel/event booking app, fintech ledger, chat app, SaaS product, recommendation system, or AI/NLP service.

[[Chapter 12 — Production Backend Architecture for Any App]]
[[Chapter 13 — PostgreSQL Production Data Modeling]]
[[Chapter 14 — Redis Production Caching, Rate Limits and Realtime State]]
[[Chapter 15 — Elasticsearch Search and Analytics]]
[[Chapter 16 — Cassandra High-Scale Data Modeling]]
[[Chapter 17 — Messaging, Events, Outbox and Sagas]]
[[Chapter 18 — AI, NLP, Vector Search and Spring AI]]
[[Chapter 19 — Observability, Performance and Production Readiness]]
[[Chapter 20 — Industry Scale System Design Reference]]
[[Chapter 21 — Spring Boot Project Architecture and Code Structure]]
[[Chapter 22 — HLD and LLD Design Playbook]]
[[Chapter 23 — Taxi Aggregator Uber Scale HLD]]
[[Chapter 24 — Food Delivery App HLD and LLD Case Study]]
[[Chapter 25 — Relationship Modeling for System Design and JPA]]

## Recommended Reading Order

1. Start with Chapters 1-5 if Spring Boot itself is still new.
2. Read Chapter 12 before designing any large app.
3. Read Chapter 13 for source-of-truth relational data.
4. Read Chapter 14 when you need speed, counters, locks, rate limits, or temporary state.
5. Read Chapter 15 when users need search, autocomplete, filters, geo search, logs, or analytics.
6. Read Chapter 16 only when a table must handle huge write volume or global scale.
7. Read Chapter 17 before adding payments, bookings, delivery assignment, notifications, or cross-service workflows.
8. Read Chapter 18 for NLP, embeddings, recommendations, semantic search, chatbots, RAG, moderation, and AI features.
9. Read Chapter 19 before calling anything production-ready.
10. Read Chapter 20 when you need system design reference architectures for delivery, Uber-like, booking, marketplace, chat, payments, search, and AI systems.
11. Read Chapter 21 before creating a real Spring Boot codebase so your folders, modules, DTOs, services, repositories, events, tests, and config stay clean.
12. Read Chapter 22 when you want the deep HLD/LLD design method with use-case diagrams, class diagrams, sequence diagrams, activity diagrams, schema design, locking, SOLID and design patterns.
13. Read Chapter 23 when you want a taxi/Uber aggregator HLD with WAF, load balancer, WebSocket location updates, region/cell matching, Kafka, analytics, ML, pricing and backup datacenter thinking.
14. Read Chapter 24 when you want a complete food delivery case study with HLD, LLD, order flow, payment flow, delivery assignment, classes, APIs, schemas and Spring Boot package mapping.
15. Read Chapter 25 when you need to decide one-to-one, one-to-many, many-to-many, join table, ownership, aggregate boundary and JPA relationship mappings for system design.

## Rule of Thumb

PostgreSQL is your default database. Redis is your speed layer. Elasticsearch is your search/read-optimized index. Cassandra is for massive predictable write/read patterns. Kafka or RabbitMQ moves work safely between services. Object storage stores files. Spring AI/vector databases power semantic retrieval. Observability tells you what is happening when production behaves differently from your laptop.
