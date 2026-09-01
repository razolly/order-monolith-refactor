/**
 * The web edge: the REST controller, its request/response DTOs ({@code dto}),
 * and the single {@code @RestControllerAdvice} that turns exceptions into
 * responses.
 *
 * <p><b>Why this package exists / separation of concerns.</b> This layer is
 * responsible for HTTP and nothing else: bind and bean-validate the request,
 * hand a typed command to {@link com.example.ordermonolith.service.OrderService},
 * map the result (or a thrown domain exception) to a status code and body. It
 * holds no business rules, no SQL, no HTTP-client calls.
 *
 * <p>DTOs here are a separate vocabulary from the JPA entities in
 * {@code persistence.entity}: the wire format can evolve without touching the
 * schema and vice versa, and the entities never get serialized to clients.
 */
package com.example.ordermonolith.web;
