# README_FIX — What Was Changed and Why

This document explains the refactor of the checkout flow from a single
`OrderController.checkout(...)` method into a layered application. It follows the
tasks in `README.md` and the "Known smells" listed in the original class header.

The observable HTTP contract is preserved: the four characterisation tests in
`OrderControllerTest.java` still pass **unchanged**. `./gradlew build` is green.

---

## 1. Summary of the shape change

**Before:** one class (`com.example.ordermonolith.OrderController`) doing request
parsing, validation, product lookup, stock checks, pricing math, payment routing,
three separate DB writes, response building, error handling and logging — inside
one `try/catch`.

**After:**

```
web/            HTTP edge: controller + DTOs + one exception handler
service/        orchestration + transaction boundaries
pricing/        tax / shipping / coupon rules (no Spring, unit-testable)
payment/        Strategy pattern, one class per provider
persistence/    JPA entities + Spring Data repositories
error/          one exception per failure mode
```

Every package has a `package-info.java` explaining its responsibility and why it
was split out.

---

## 2. Fixes by concern

### 2.1 Untyped request → typed, validated DTOs

**Problem.** The request was `Map<String, Object>`. A typo in a JSON key failed
silently or as an NPE → 500. "Validation" was a ~7-level `if/else` pyramid that
string-sniffed for `"@"` and `"."` to check an email.

**Fix.**

- `web/dto/CheckoutRequest.java` and `CheckoutItem.java` — Java records with
  Jakarta Bean Validation:
  - `@NotBlank @Email` on `customerEmail`
  - `@NotEmpty @Valid` on `items` (cascades into each line)
  - `@NotNull @Min(1)` on `productId` and `quantity`
  - `paymentMethod` is the `PaymentMethod` **enum** — an unknown value fails to
    deserialize and becomes a 400, so the "is it one of these three strings?"
    check disappears.
- Conditional validation for card details lives in a dedicated class-level
  constraint, `web/dto/CardDetailsPresentForCreditCard.java` (+ its validator),
  rather than an inline accessor — it is named, reusable, and reports as a
  request-level error instead of an error on a synthetic field.
- The whole pyramid is gone. Bean validation runs **before** the controller
  method, so bad input never reaches business logic.

**Why records.** Immutable, no Lombok needed, and the accessor names are the
wire contract in one place.

### 2.2 Hand-built response map → response DTO

**Problem.** The response was a `LinkedHashMap` assembled field by field.

**Fix.** `web/dto/CheckoutResponse.java` — a record with a `from(Order)` factory
so the entity → DTO mapping is in one place and the JPA entity is never
serialized to clients. The JSON shape is unchanged (`orderId`, `status`,
`lines`, `subtotal` … `total`), which is why the existing tests still pass.

### 2.3 Fat controller → thin controller + service

**Problem.** Orchestration *was* the controller method.

**Fix.**

- `web/OrderController.java` is now: validate → map `CheckoutRequest` to
  `service/CheckoutCommand` → delegate → map result. No branching, no SQL, no
  HTTP client, no `try/catch`.
- `service/OrderService` (interface) + `service/DefaultOrderService`
  (implementation) own the sequence. The controller and tests depend on the
  interface.
- `CheckoutCommand` is a plain value object, so **no `jakarta.*` / Jackson / MVC
  type crosses into the service layer**.

`DefaultOrderService.checkout(...)` reads as four steps:

1. `ProductCatalog.toCartLines(...)` — look up + price each line
2. `PricingCalculator.price(...)` — order-level tax / shipping / discount
3. `PaymentStrategyRegistry.resolve(method).charge(...)` — charge the customer
4. `OrderWriter.persist(...)` — write the order atomically

### 2.4 Inline JDBC → repositories and entities

**Problem.** `JdbcTemplate.queryForList` in a loop, reading `Map` columns by
UPPER-CASE key; after inserting the order it re-selected the generated id with
`SELECT id ... ORDER BY id DESC LIMIT 1` (racy).

**Fix.**

- `persistence/entity/` — `Product`, `Order`, `OrderItem`, `AuditLogEntry`
  mapped to the existing `schema.sql` tables. `Order` is the aggregate root:
  `addItem(...)` builds children and `cascade = ALL` persists them in one unit
  of work, so there is no id re-query.
- `persistence/repository/` — Spring Data interfaces returning typed entities.
- `status` is an `OrderStatus` enum, `payment_method` stored as a string via the
  strategy — no more magic `"CONFIRMED"` literal.
- Entities stay inside the service layer; the web layer only sees DTOs.

### 2.5 Magic numbers in pricing → configuration + named rules

**Problem.** `0.08` tax, `100` free-shipping threshold, `9.99` shipping,
`"SAVE5"` coupon — all inline `BigDecimal` math in the handler.

**Fix.**

- `pricing/PricingProperties.java` — `@ConfigurationProperties(prefix = "pricing")`
  bound from `application.yml`:
  ```yaml
  pricing:
    tax-rate: 0.08
    free-shipping-threshold: 100.00
    standard-shipping-fee: 9.99
    coupons:
      SAVE5: 0.05
  ```
- `pricing/PricingCalculator.java` — one private method per rule
  (`tax`, `shipping`, `discount`), composed in `price(...)`. No literals in the
  flow. No Spring/JPA/web imports, so `new PricingCalculator(props)` works in a
  plain JUnit test.
- `pricing/CartLine` and `pricing/PriceBreakdown` are immutable records; all
  money is rounded `HALF_UP` to 2 dp in one helper.

### 2.6 `switch` on payment method → Strategy pattern

**Problem.** `switch (paymentMethod)` with an inline `RestTemplate` call in every
branch (PayPal `new`-ed its own client), a hardcoded
`Bearer sk_live_HARDCODED_STRIPE_KEY`, and every `catch` swallowed the error and
fabricated a reference so the order "succeeded" anyway. Adding a provider meant
editing the switch **and** the validation pyramid.

**Fix.**

- `payment/PaymentStrategy` — `method()` + `charge(PaymentCommand)`.
- `payment/StripePaymentStrategy`, `PaypalPaymentStrategy`,
  `CreditCardPaymentStrategy` — each a `@Component`. Stripe and PayPal share
  `HttpPaymentStrategy` (one injected `RestTemplate`, shared error handling and
  the fake-approval switch).
- `payment/PaymentStrategyRegistry` — Spring injects **all** strategies as a
  list; the registry indexes them by `method()` into an `EnumMap` at
  construction and fails fast if two claim the same method. Resolution is a map
  lookup — **no `switch`, no `if/else`**.
- **Adding a fourth provider = one new class.** The controller, service and
  registry do not change.
- Config is externalised (`payment/PaymentProperties`, `payment.*` in yml);
  API keys come from the environment (`${STRIPE_API_KEY:}`), not code.
- Card numbers are **never logged** — not even the last four digits.
- `payment.fake-approvals` makes the previously-hidden fake behaviour explicit:
  `true` for local/dev (skip the non-existent gateway, return a synthetic
  reference), `false` where a real outage must surface as a 502.

### 2.7 No transaction → atomic write path

**Problem.** Three separate `JdbcTemplate` write groups with no `@Transactional`.
If the `order_items` loop or the `audit_log` insert threw after payment
succeeded, the customer was charged and the order was left half-written — the
"money taken, no order" incidents from the scenario.

**Fix.**

- `service/OrderWriter.persist(...)` is `@Transactional` and does **all** writes
  — order + items (cascade) + stock decrement + audit — in one transaction. Any
  exception rolls the whole thing back.
- It is a **separate bean** on purpose: `@Transactional` only applies when a
  method is called through the Spring proxy, so this could not be a private
  method of `DefaultOrderService`.
- **Read vs write is deliberate:** `ProductCatalog` is
  `@Transactional(readOnly = true)`; `OrderWriter` is read-write.
- **The remote payment call is outside any DB transaction.** The service charges
  the customer *before* calling `OrderWriter`, so a DB transaction is never held
  open across an HTTP round-trip.
- **Known residual trade-off (documented in `OrderWriter`):** payment commits
  before the DB does, so a failure in `persist(...)` still means "charged, no
  order". The transaction guarantees the database is all-or-nothing; a
  production system would enqueue a refund/compensation on that path. The seam
  (one method, one place to react) now exists.

### 2.8 Oversell race

**Problem.** "Read stock, compare, then `UPDATE stock = stock - ?`" has a
window where two concurrent checkouts both pass the check.

**Fix.** `ProductRepository.decrementStock(...)`:

```sql
UPDATE Product p SET p.stock = p.stock - :quantity
 WHERE p.id = :productId AND p.stock >= :quantity
```

The `AND p.stock >= :quantity` guard makes it a compare-and-set. The database
serialises the row update; the loser matches **0 rows** and the caller turns
that into `InsufficientStockException` (422) and rolls back. No schema change
for a `@Version` column was needed. The earlier check in `ProductCatalog` is
kept only as a fast-fail for the common case.

### 2.9 Catch-all 500 → global exception handling with correct status codes

**Problem.** `catch (Exception e)` turned bad input, unknown product, out of
stock and a dropped DB connection all into
`ResponseEntity.status(500).body("Error: " + e.getMessage())`. The caller could
not tell a retryable failure from a permanent one.

**Fix.**

- `error/` — one exception per failure mode:
  `ProductNotFoundException`, `InsufficientStockException`,
  `InvalidCheckoutException`, `PaymentGatewayException` (all extend
  `CheckoutException`). None of them know about HTTP.
- `web/GlobalExceptionHandler` — a single `@RestControllerAdvice` that maps them
  to RFC 7807 `ProblemDetail` bodies:

  | Situation | Status |
  |---|---|
  | Bean-validation failure / malformed body | `400 Bad Request` |
  | Unknown product | `404 Not Found` |
  | Out of stock / invalid card | `422 Unprocessable Entity` |
  | Payment provider down / unreachable | `502 Bad Gateway` |

- Infrastructure failures have **no** handler, so they keep Spring's default 500
  — we do not dress them up as something the caller can act on.
- The controller's `try/catch` is deleted.

### 2.10 Noisy / unsafe logging

**Problem.** `log.info` scattered through the method; it logged the full request
body **including `cardNumber`**, and logged "card ending 1111".

**Fix.** One completion log line in the service; payment strategies log that a
charge happened, never the PAN. The full-body log is gone.

### 2.11 Object construction — Lombok builders, no positional constructors

**Problem.** Several new value types and entities are built from many arguments,
some of the same type (`PaymentCommand` has `customerEmail` and `cardNumber`
adjacent, both `String`). Positional `new X(a, b, c, d, e)` calls are easy to get
wrong and hard to read at the call site.

**Fix.**

- Value records (`CartLine`, `PriceBreakdown`, `PaymentCommand`, `PaymentResult`,
  `CheckoutCommand` + `Item`, `CheckoutResponse` + `Line`) and the config
  records (`PricingProperties`, `PaymentProperties`) carry `@Builder`.
- Entities (`Order`, `OrderItem`, `AuditLogEntry`) are built with `@Builder` too:
  - `Order` puts `@Builder` on a **private constructor** that takes only the
    business fields — `id` is DB-assigned, `status` is always `CONFIRMED`,
    `createdAt` is stamped in the constructor, `items` starts empty.
  - `@NoArgsConstructor(access = PROTECTED)` is kept for Hibernate.
  - The hand-written `Order.confirmed(...)` / `AuditLogEntry.now(...)` factories
    are gone; call sites use `Order.builder()...build()` etc.
- Every construction site (production and tests) now reads as named
  `.field(value)` calls.
- DI constructors that were pure field assignment (`OrderController`,
  `PricingCalculator`, `ProductCatalog`, `OrderWriter`, `DefaultOrderService`)
  use `@RequiredArgsConstructor` / `@AllArgsConstructor`. `PaymentStrategyRegistry`
  keeps an explicit constructor because it does work (building the method → bean
  map).

---

## 3. Tests added

| Test | Type | Proves |
|---|---|---|
| `pricing/PricingCalculatorTest` | pure unit (no Spring) | each pricing rule and the final composition |
| `payment/PaymentStrategyRegistryTest` | unit | correct resolution, duplicate detection, unknown method, short-PAN rejection |
| `payment/StripePaymentStrategyTest` | unit + Mockito | fake mode skips HTTP; real response id is used; transport failure → `PaymentGatewayException` |
| `CheckoutAtomicityTest` | `@SpringBootTest` | forcing the audit write to fail leaves **no order and no stock change** |
| `OrderControllerTest` (existing) | `@SpringBootTest` | happy path + 404 + 422 + 400 — kept unchanged |

---

## 4. Config / wiring changes

- `OrderMonolithApplication` — added `@ConfigurationPropertiesScan`; kept the
  single shared `RestTemplate` bean (now injected into every strategy).
- `application.yml` — added the `pricing.*` and `payment.*` blocks; removed the
  `org.springframework.jdbc.core: DEBUG` log level (no JDBC left).
- No `build.gradle` changes — `spring-boot-starter-validation`,
  `-data-jpa` and `-web` were already present.

---

## 5. What was deliberately *not* done

- **Retry / refund on the post-payment failure path** — noted as a trade-off in
  `OrderWriter`; needs a queue and is out of scope for this exercise.
- **`@Version` optimistic locking** — the conditional update covers the stated
  oversell risk without a schema migration.
- **Splitting `schema.sql` / Flyway** — the H2 bootstrap is unchanged.
- **Idempotency keys on checkout** — worth doing for a real payment endpoint,
  but not in the task list.
