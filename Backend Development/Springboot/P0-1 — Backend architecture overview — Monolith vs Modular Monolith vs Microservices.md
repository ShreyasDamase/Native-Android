# P0-1 — Backend Architecture Overview

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> [!WARNING]
> **The Startup Killer**
> Picking the wrong architecture at the start of your company will kill your engineering velocity. Over-engineering with Microservices on Day 1 means you spend 80% of your time managing infrastructure instead of building product. Under-engineering a Monolith means you cannot scale when you finally get real traction.

To build a production-level company, you must understand the spectrum of architectures. You don't build microservices because "Netflix does it." You build what solves your current organizational scale.

---

## 1. The Monolith

A monolith is a single deployable unit containing all your backend code (e.g., a single Spring Boot `.jar` file).

### How it Works
All modules (Users, Orders, Inventory, Payments) share the same memory space, the same database, and the same code repository. A function call from `OrderService` to `InventoryService` is a direct CPU-level method invocation (nanoseconds), not a network call (milliseconds).

### Why Use It? (The Default Startup Choice)
- **Extreme Velocity:** You can refactor across domains with a single IDE click.
- **Transactional Safety:** You have single ACID database transactions. If an order fails, the inventory decrement rolls back automatically.
- **Easy Deployment:** You only have to deploy one container, configure one CI/CD pipeline, and monitor one set of logs.

### Why it Fails at Scale
- **The "Big Ball of Mud":** Developers get lazy and `OrderService` directly queries `UserRepository`. Domains bleed into each other.
- **Deploy Risk:** A tiny bug in the payment gateway code can crash the entire system, taking down user login and inventory as well.
- **Scaling Limits:** You cannot independently scale the specific module that gets heavy traffic. You have to clone the entire massive monolith.

---

## 2. The Modular Monolith (The Sweet Spot)

> [!IMPORTANT]
> **This is the recommended architecture for 99% of new companies today.**

A Modular Monolith is deployed as a single Spring Boot app, but the codebase is strictly segregated into independent logical modules.

### How it Works
You enforce boundaries internally. The `Order` module cannot directly query the `User` database tables. Instead, `Order` must fire an in-memory event (`UserCreatedEvent`) or call a strict `UserFacade` interface. 

### Why Use It?
- **Microservices Ready:** Because the code is already decoupled, if the `Payment` module gets too heavy in year 3, you can easily cut it out and deploy it as a separate Microservice.
- **No Network Tax:** You still get the speed of in-memory method calls, and you don't have to deal with distributed tracing, network latency, or partial failures.
- **Developer Discipline:** It forces your team to design clean domain boundaries (Domain Driven Design) without paying the DevOps tax of Kubernetes orchestration right away.

---

## 3. Microservices

Microservices divide the application into many small, independently deployable services (e.g., `user-service.jar`, `order-service.jar`, `inventory-service.jar`).

### How it Works
Services communicate exclusively over the network using REST (synchronous) or message brokers like Kafka/RabbitMQ (asynchronous). Each microservice **must** own its own database. `OrderService` cannot reach into the `Inventory` database. 

### Why Use It? (The Enterprise Choice)
- **Organizational Scaling:** The primary reason for microservices is team size. When you have 50+ developers, you cannot all work in one codebase without stepping on each other's toes. You assign Team A to Orders and Team B to Inventory.
- **Independent Deployments:** The Payment team can deploy 10 times a day without coordinating with the Orders team.
- **Fault Isolation:** If the Recommendation Engine crashes due to a memory leak, the core Checkout system stays online.

### The Hidden Horrors (Why it kills early startups)
> [!CAUTION]
> Microservices introduce **Distributed Systems Complexity**.
- **No ACID Transactions:** You cannot just `@Transactional` across microservices. You must implement complex paradigms like the **Saga Pattern** or **Outbox Pattern** to ensure data consistency.
- **Network Unreliability:** Network calls fail. You now need retries, circuit breakers (Resilience4j), and dead-letter queues.
- **Operational Nightmare:** Debugging requires distributed tracing (Zipkin/Jaeger) because a single user request jumps through 5 different servers before returning.

---

## Summary Checklist for Founders

1. **Are you pre-product-market fit?** Build a Monolith. Focus on features.
2. **Do you have a small engineering team (1-10 devs) but want clean code?** Build a Modular Monolith.
3. **Are you scaling to hundreds of developers and millions of users with specific bottlenecked domains?** Transition to Microservices, one domain at a time.
