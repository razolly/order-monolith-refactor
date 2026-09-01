package com.example.ordermonolith.web.dto;

import com.example.ordermonolith.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The checkout request body. Replaces the untyped {@code Map<String, Object>}
 * and the seven-level {@code if/else} validation pyramid: every rule that
 * pyramid enforced is now a declarative constraint, checked before the
 * controller method runs. A failure becomes {@code 400 Bad Request} via
 * {@link com.example.ordermonolith.web.GlobalExceptionHandler}.
 *
 * @param customerEmail buyer's email, must look like an address
 * @param items         at least one line, each individually valid
 * @param paymentMethod one of the {@link PaymentMethod} values; an unknown
 *                      string fails to bind and is a 400
 * @param coupon        optional discount code
 * @param cardNumber    required only for {@link PaymentMethod#CREDIT_CARD}
 *                      (see {@link CardDetailsPresentForCreditCard}); its
 *                      <em>validity</em> is a business rule checked in the
 *                      payment strategy (422), not here
 */
@CardDetailsPresentForCreditCard
public record CheckoutRequest(

        @NotBlank(message = "customerEmail is required")
        @Email(message = "customerEmail is not a valid email")
        String customerEmail,

        @NotEmpty(message = "items must not be empty")
        @Valid
        List<CheckoutItem> items,

        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,

        String coupon,

        String cardNumber
) {
}
