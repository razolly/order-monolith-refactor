package com.example.ordermonolith.web.dto;

import com.example.ordermonolith.payment.PaymentMethod;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class CardDetailsPresentForCreditCardValidator
        implements ConstraintValidator<CardDetailsPresentForCreditCard, CheckoutRequest> {

    @Override
    public boolean isValid(CheckoutRequest request, ConstraintValidatorContext context) {
        if (request == null || request.paymentMethod() != PaymentMethod.CREDIT_CARD) {
            return true;
        }
        return request.cardNumber() != null && !request.cardNumber().isBlank();
    }
}
