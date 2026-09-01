package com.example.ordermonolith.web.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class-level constraint: a {@link CheckoutRequest} for
 * {@link com.example.ordermonolith.payment.PaymentMethod#CREDIT_CARD} must carry
 * a non-blank {@code cardNumber}.
 *
 * <p>A dedicated constraint (rather than an inline {@code @AssertTrue} accessor)
 * keeps the rule named and reusable, and it reports as a request-level error
 * instead of an error on a synthetic field. The card's <em>validity</em> stays a
 * business rule in the payment strategy (a 422), not a bind-time check.
 */
@Documented
@Constraint(validatedBy = CardDetailsPresentForCreditCardValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface CardDetailsPresentForCreditCard {

    String message() default "cardNumber is required for CREDIT_CARD payments";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
