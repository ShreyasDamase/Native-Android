# M6 — Infrastructure

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

The book correction: Docker Compose is not just an external script. Spring Boot 3.1+ can integrate with Docker Compose in development and auto-configure backing services.

## Implementation Order

6.1 Dockerize Spring Boot app + PostgreSQL with Docker Compose.

6.2 Add `spring-boot-docker-compose` for local development.

6.3 Add Redis:

- Product catalog cache
- Session/current cart cache
- Rate limit counters

6.4 Configure NGINX reverse proxy + SSL termination + rate limiting.

6.5 Integrate MinIO / Cloudflare R2 for product image storage.

6.6 Set up Elasticsearch:

- Product index
- Sync from PostgreSQL
- Full text
- Filters
- Autocomplete
- Ranking

6.7 Add Spring Cloud Gateway only when you need application-level gateway features:

- Route predicates
- Filters
- Service discovery integration
- Auth propagation

## Dev vs Production Rule

Docker Compose auto-configuration is excellent for local development. Production should use explicit environment variables, secrets, service discovery, and health checks.
