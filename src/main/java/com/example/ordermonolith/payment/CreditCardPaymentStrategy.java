package com.example.ordermonolith.payment;

import com.example.ordermonolith.error.InvalidCheckoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * In-process "card processor". Stands in for a real acquirer integration.
 *
 * <p>Two deliberate changes from the monolith: it validates the PAN (a business
 * rule &rarr; {@link InvalidCheckoutException} &rarr; {@code 422}), and it never
 * logs the card number, not even the last four digits.
 */
@Component
class CreditCardPaymentStrategy implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(CreditCardPaymentStrategy.class);
    private static final int MIN_PAN_LENGTH = 12;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CREDIT_CARD;
    }

    @Override
    public PaymentResult charge(PaymentCommand command) {
        String pan = command.cardNumber() == null ? "" : command.cardNumber().replaceAll("\\s", "");
        if (pan.length() < MIN_PAN_LENGTH) {
            throw new InvalidCheckoutException("cardNumber looks invalid");
        }
        log.info("Authorising card payment of {} for {}", command.amount(), command.customerEmail());
        return PaymentResult.builder()
                .reference("cc_" + UUID.randomUUID())
                .provider("credit_card")
                .build();
    }
}
