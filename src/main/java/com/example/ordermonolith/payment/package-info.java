/**
 * Payment integration, isolated behind the {@link com.example.ordermonolith.payment.PaymentStrategy}
 * interface (Strategy pattern).
 *
 * <p><b>Why this package exists.</b> In the monolith, payment routing was a
 * {@code switch (paymentMethod)} with an inline {@code RestTemplate} call in
 * every branch (one branch {@code new}-ing its own client), a hardcoded API key,
 * and swallowed exceptions. Adding a provider meant editing the switch <em>and</em>
 * the validation pyramid, and a change to one branch could regress another.
 *
 * <p>Now each provider is a self-contained {@code @Component} implementing one
 * interface. {@link com.example.ordermonolith.payment.PaymentStrategyRegistry}
 * resolves the right one from an injected collection &ndash; no {@code switch},
 * no {@code if/else}. Adding a fourth provider is a new class and nothing else:
 * the controller and service never mention concrete providers.
 *
 * <p>Everything the providers talk in ({@link com.example.ordermonolith.payment.PaymentCommand},
 * {@link com.example.ordermonolith.payment.PaymentResult}) is local to this
 * package so the payment concern never leaks web DTOs or JPA entities.
 */
package com.example.ordermonolith.payment;
