# Order Checkout — Refactoring Exercise (Senior Java)

> **Format:** ~90 minute pair-programming session.
> You will not finish every task, and that is fine. We care about how you
> reason, sequence the work, and keep the build green — not about volume.

---

## 1. Context & Scenario

You have just joined the **Checkout** team at a mid-size e-commerce company.

Eighteen months ago a single engineer built a "quick prototype" of the checkout
flow to unblock a demo. The demo went well. The prototype went to production.
It has been there ever since, and more logic has been piled onto it every
sprint.

It is now the single most business-critical endpoint we own, and it is a
**single controller method**. It works on the happy path, but:

- Incidents are frequent and hard to diagnose.
- We have had at least two "money taken, no order created" support escalations.
- Adding a new payment provider last quarter took a week and caused a regression
  in an unrelated provider.
- Nobody wants to touch it.

Leadership has approved a hardening effort. You are pairing with one of our
engineers to start that work.

The code lives in `src/main/java/com/example/ordermonolith/OrderController.java`.

---

## 2. Current Architecture Overview

There is no architecture. There is one class.

`POST /api/v1/orders/checkout` accepts an untyped JSON object and, inside a
single `try/catch`, does all of the following in sequence:

| Concern | How it is done today |
|---|---|
| **Request parsing / validation** | `Map<String, Object>` + a ~7-level-deep `if/else` pyramid, string-sniffing for `"@"` to "validate" email |
| **Product lookup & stock check** | Inline `JdbcTemplate.queryForList` in a loop, reading `Map` columns by uppercase key |
| **Pricing** | Inline `BigDecimal` math with magic numbers (8% tax, `9.99` shipping, `"SAVE5"` coupon) |
| **Payment** | A `switch (paymentMethod)` over `"STRIPE"`, `"PAYPAL"`, `"CREDIT_CARD"`, each branch doing its own inline `RestTemplate` call (one branch `new RestTemplate()`s its own), hardcoded API key, exceptions swallowed and faked |
| **Persistence** | Three separate `JdbcTemplate.update` groups — insert `orders`, insert `order_items` + decrement `products.stock`, insert `audit_log` — **with no `@Transactional` boundary** |
| **Response** | Hand-built `LinkedHashMap` |
| **Error handling** | One catch-all returning `ResponseEntity.status(500).body("Error: " + e.getMessage())` for everything from bad input to a dropped DB connection |
| **Logging** | `log.info(...)` scattered through the method, logs the full request body (including `cardNumber`) |

### Known correctness / safety problems

1. **Partial writes.** If the `order_items` loop or the `audit_log` insert throws
   after payment succeeded, the customer is charged but the order is inconsistent.
2. **Lost error semantics.** "Product not found", "out of stock", and "payment
   gateway down" are indistinguishable to the caller in the failure case — and
   the success-path validation returns bare strings with ad-hoc status codes.
3. **Open/Closed violation.** Every new payment provider means editing the
   `switch` and the validation pyramid.
4. **Untyped everything.** No compile-time safety on the request or response;
   a typo in a JSON key fails silently or with an NPE → 500.

---

## 3. Candidate Objectives

Work top-to-bottom. Commit (or checkpoint) after each task. Keep
`./gradlew test` green — extend the tests as the contract legitimately changes.

### Task 1 — Refactor the Controller Layer to typed, validated DTOs
- Replace the `Map<String, Object>` request with a `CheckoutRequest` DTO
  (records are fine) and nested `CheckoutItem`.
- Add Jakarta Bean Validation: `@NotBlank` / `@Email` on customer email,
  `@NotEmpty` on items, `@NotNull` + `@Min(1)` on quantity and product id,
  conditional validation for card details when `paymentMethod == CREDIT_CARD`.
- Replace the hand-built response `Map` with a `CheckoutResponse` DTO.
- The controller method should end up small enough to read at a glance.

### Task 2 — Extract Service & Repository layers
- Introduce JPA entities (`Product`, `Order`, `OrderItem`, `AuditLog`) and
  Spring Data `Repository` interfaces. Drop the inline `JdbcTemplate` usage.
- Move orchestration into an `OrderService` (interface + implementation).
- Move pricing into its own collaborator (e.g. `PricingCalculator`) with the
  tax / shipping / coupon rules as named, testable units — no magic numbers
  in the flow.
- Apply `@Transactional` correctly:
  - the checkout write path is **read-write** and must be atomic;
  - pure lookups should be `@Transactional(readOnly = true)`;
  - be deliberate about where the payment call sits relative to the
    transaction boundary (do not hold a DB transaction open across a remote
    HTTP call — be ready to discuss the trade-off).

### Task 3 — Strategy pattern for payments
- Define a `PaymentStrategy` interface (e.g. `PaymentResult charge(PaymentCommand cmd)`).
- One implementation per provider (`StripePaymentStrategy`, `PaypalPaymentStrategy`,
  `CreditCardPaymentStrategy`), each a Spring `@Component`.
- Resolve the strategy via an injected `Map<String, PaymentStrategy>` (or a
  small registry) keyed by payment method — **no `switch`, no `if/else` chain**.
- Adding a fourth provider must require **zero changes** to the controller or
  service.
- Externalise config (API base URL, keys) into `application.yml` /
  `@ConfigurationProperties`. Stop logging card numbers.

### Task 4 — Global exception handling
- Introduce domain exceptions (`ProductNotFoundException`,
  `InsufficientStockException`, `PaymentGatewayException`,
  `InvalidCheckoutException`, …).
- Add a `@RestControllerAdvice` that maps them to a consistent error body
  (consider `ProblemDetail` / RFC 7807):

  | Situation | Status |
  |---|---|
  | Bean-validation failure / malformed request | `400 Bad Request` |
  | Unknown product | `404 Not Found` |
  | Business rule rejected (out of stock, invalid card) | `422 Unprocessable Entity` |
  | Downstream payment provider failed / unavailable | `502 Bad Gateway` |

- Remove the catch-all `catch (Exception e)` from the controller.

### Task 5 — Testing
- Keep / adapt the provided tests in
  `src/test/java/com/example/ordermonolith/OrderControllerTest.java`.
- Add at least:
  - a **unit test** for the pricing logic (no Spring context);
  - a **unit test** for strategy resolution and one payment strategy with a
    mocked HTTP client (Mockito);
  - one **slice or integration test** (`@WebMvcTest` or `@SpringBootTest`)
    proving the refactored happy path and at least one mapped error status.
- Add a test that demonstrates the checkout write path is atomic (a failure
  after "payment" leaves no order / no stock change).

---

## 4. Evaluation Criteria

You are **not** expected to complete all five tasks. A strong candidate
typically finishes Tasks 1–3 cleanly with tests. We are looking for:

### Clean Code
- [ ] Small, single-purpose classes and methods; intention-revealing names
- [ ] No magic numbers / magic strings in flow logic
- [ ] Layer boundaries respected (web ↔ service ↔ persistence ↔ integration)
- [ ] Dead code, swallowed exceptions and noisy logging removed
- [ ] Secrets and PII (API keys, card numbers) kept out of code and logs

### SOLID / Design
- [ ] Payment routing is Open/Closed — new provider = new class, no edits
- [ ] Dependencies inverted behind interfaces; injected, not `new`-ed
- [ ] Pricing and payment are independently testable collaborators
- [ ] DTOs vs entities vs domain model are distinct and not leaked across layers

### Concurrency / Transaction Safety
- [ ] Checkout write path is atomic under `@Transactional`
- [ ] Read-only vs read-write transactions used deliberately
- [ ] Remote payment call is not wrapped inside an open DB transaction
      (or the candidate can articulate why they chose otherwise)
- [ ] Stock decrement is safe against oversell (optimistic lock / conditional
      update / discussion of the race is acceptable)

### Extensibility
- [ ] Adding a payment provider or a pricing rule is a local, obvious change
- [ ] Configuration is externalised, not hardcoded

### Testability & Tests
- [ ] Core logic unit-testable without Spring
- [ ] Meaningful assertions, not just status codes
- [ ] Error-mapping behaviour is covered
- [ ] Build stays green: `./gradlew test`

### Communication (pairing)
- [ ] Explains trade-offs, asks about unknowns, sequences the work sensibly
- [ ] Makes small, reviewable steps rather than one big-bang rewrite

---

## 5. Getting Started

### Prerequisites
- JDK 17 or 21. The Gradle build targets a **Java 21 toolchain**; if you only
  have 17, change `JavaLanguageVersion.of(21)` to `17` in `build.gradle`.
- No local Gradle needed — use the wrapper.

> **Note (this machine):** `gradle.properties` pins the Gradle daemon to a
> locally installed Amazon Corretto 21 because the default `JAVA_HOME` here is
> JDK 26, which the bundled Gradle version cannot run on. On a normal setup with
> `JAVA_HOME` on Java 17/21 you can delete the `org.gradle.java.home` line.

### Build & test
```bash
./gradlew build      # compile + run tests
./gradlew test       # tests only
./gradlew bootRun    # start the app on http://localhost:8080
```

### Seed data
H2 in-memory DB, recreated on every start from `src/main/resources/schema.sql`
and `data.sql`. Five products are seeded (ids `1`–`5`); product `5`
("Laptop Stand") has only `3` in stock — handy for testing the out-of-stock
path. H2 console: `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:orders`, user `sa`, no password).

### Try the endpoint
```bash
curl -s -X POST http://localhost:8080/api/v1/orders/checkout \
  -H 'Content-Type: application/json' \
  -d '{
        "customerEmail": "buyer@example.com",
        "paymentMethod": "CREDIT_CARD",
        "cardNumber": "4111111111111111",
        "coupon": "SAVE5",
        "items": [
          { "productId": 1, "quantity": 2 },
          { "productId": 2, "quantity": 1 }
        ]
      }' | jq
```

The `STRIPE` and `PAYPAL` methods point at a non-existent
`https://api.paymentservice.com/v1/charge`; the current code catches the
failure and fabricates a payment reference so the flow still completes. Part of
Task 3 is to make that behaviour explicit and configurable (e.g. a fake/local
strategy for dev, real HTTP for prod).

---

## 6. Project Layout

```
src/main/java/com/example/ordermonolith/
├── OrderMonolithApplication.java   # Spring Boot entrypoint + RestTemplate bean
└── OrderController.java            # <-- the monolith. Start here.

src/main/resources/
├── application.yml                 # H2 + SQL init config
├── schema.sql                      # tables: products, orders, order_items, audit_log
└── data.sql                        # seed products

src/test/java/com/example/ordermonolith/
└── OrderControllerTest.java        # characterisation tests for current behaviour
```

Good luck — talk us through your thinking as you go.
