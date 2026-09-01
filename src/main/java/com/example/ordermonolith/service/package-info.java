/**
 * Application/orchestration layer.
 *
 * <p><b>Why this package exists.</b> The monolith's controller method <em>was</em>
 * the orchestration &ndash; it strung together lookup, pricing, payment and three
 * writes in one {@code try}. {@link com.example.ordermonolith.service.OrderService}
 * now owns that sequence and nothing else: it calls the pricing collaborator, the
 * payment strategy and the repositories in the right order and with the right
 * transaction boundaries. It has no HTTP types (it speaks
 * {@link com.example.ordermonolith.service.CheckoutCommand}, not the web DTO) and
 * no SQL.
 *
 * <p>The interface/implementation split ({@code OrderService} /
 * {@code DefaultOrderService}) lets the controller and tests depend on the
 * abstraction. The transactional write is deliberately a <em>separate</em> bean
 * ({@link com.example.ordermonolith.service.OrderWriter}) so its
 * {@code @Transactional} boundary is a real proxy boundary and the remote payment
 * call stays outside it.
 */
package com.example.ordermonolith.service;
