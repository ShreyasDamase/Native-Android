# M7 — Observability & Operations

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

The book correction: start with Spring Boot Actuator. Prometheus/Grafana come after Actuator exposes health and metrics.

## Implementation Order

7.1 Add `spring-boot-starter-actuator`.

7.2 Configure Actuator endpoints safely:

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/info`
- `/actuator/prometheus` when Prometheus registry is added

7.3 Add custom health indicators for:

- PostgreSQL
- Redis
- RabbitMQ/Kafka
- Payment provider sandbox

7.4 Implement structured logging with correlation IDs.

7.5 Set up Prometheus + Grafana dashboards.

7.6 Add tracing with Micrometer/OpenTelemetry.

7.7 Set up GitHub Actions CI/CD:

- Build
- Test
- Package
- Deploy

7.8 Run k6 load tests and document bottlenecks.

7.9 Write production operations runbook and incident response plan.

## Security Rule

Never expose all Actuator endpoints publicly. Health can be public-ish behind infra; env/configprops/beans must be protected.
