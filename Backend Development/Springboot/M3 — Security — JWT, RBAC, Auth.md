# M3 — Security — JWT, RBAC, Auth

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

The book's correction: do not start with JWT as if JWT is Spring Security. Start with Spring Security's architecture:

- `SecurityFilterChain`
- `HttpSecurity`
- Authentication
- Authorization
- `UserDetails`
- `PasswordEncoder` / `BCryptPasswordEncoder`
- CORS
- Security tests

Then add JWT as the stateless REST API token strategy.

## Implementation Order

3.1 Add `spring-boot-starter-security`.

3.2 Create a minimal `SecurityFilterChain`.

3.3 Add password hashing with `BCryptPasswordEncoder`.

3.4 Implement user registration and login endpoints.

3.5 Implement JWT generation, validation, and refresh token.

3.6 Implement RBAC:

- Customer
- Store Admin
- Delivery Partner
- Admin

3.7 Secure endpoints with route rules and method-level authorization.

3.8 Add security tests:

- Public endpoint accessible
- Protected endpoint rejects unauthenticated request
- Wrong role receives 403
- Correct role succeeds

## Common Mistake

Do not store passwords in plain text and do not put authorization logic only in controllers. Security belongs in the filter chain and method/service boundaries.
