/**
 * Domain-level exception types and the single HTTP translation point for them.
 *
 * <p><b>Why this package exists.</b> The original controller signalled every
 * failure the same way &ndash; {@code ResponseEntity.status(500).body("Error: ...")}
 * &ndash; so "product not found", "out of stock" and "payment gateway down" were
 * indistinguishable to the caller. Here each failure mode is its own type thrown
 * by the layer that detects it (service, pricing, payment). None of these classes
 * know about HTTP; {@link com.example.ordermonolith.web.GlobalExceptionHandler}
 * is the one place that maps them to status codes, keeping the web concern out of
 * the domain.
 */
package com.example.ordermonolith.error;
