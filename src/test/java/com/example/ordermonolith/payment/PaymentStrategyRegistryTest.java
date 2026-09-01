package com.example.ordermonolith.payment;

import com.example.ordermonolith.error.InvalidCheckoutException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit test for strategy resolution - no Spring context. */
class PaymentStrategyRegistryTest {

    private static PaymentStrategy fake(PaymentMethod method) {
        return new PaymentStrategy() {
            @Override
            public PaymentMethod method() {
                return method;
            }

            @Override
            public PaymentResult charge(PaymentCommand command) {
                return PaymentResult.builder().reference("ref").provider(method.name()).build();
            }
        };
    }

    @Test
    void resolvesEachMethodToItsStrategy() {
        PaymentStrategy stripe = fake(PaymentMethod.STRIPE);
        PaymentStrategy card = fake(PaymentMethod.CREDIT_CARD);
        PaymentStrategyRegistry registry = new PaymentStrategyRegistry(List.of(stripe, card));

        assertThat(registry.resolve(PaymentMethod.STRIPE)).isSameAs(stripe);
        assertThat(registry.resolve(PaymentMethod.CREDIT_CARD)).isSameAs(card);
    }

    @Test
    void rejectsAnUnregisteredMethod() {
        PaymentStrategyRegistry registry = new PaymentStrategyRegistry(List.of(fake(PaymentMethod.STRIPE)));

        assertThatThrownBy(() -> registry.resolve(PaymentMethod.PAYPAL))
                .isInstanceOf(InvalidCheckoutException.class);
    }

    @Test
    void failsFastWhenTwoStrategiesClaimTheSameMethod() {
        assertThatThrownBy(() -> new PaymentStrategyRegistry(
                List.of(fake(PaymentMethod.STRIPE), fake(PaymentMethod.STRIPE))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void creditCardStrategyRejectsAShortPan() {
        CreditCardPaymentStrategy strategy = new CreditCardPaymentStrategy();

        assertThatThrownBy(() -> strategy.charge(PaymentCommand.builder()
                .method(PaymentMethod.CREDIT_CARD)
                .amount(new BigDecimal("10.00"))
                .currency("usd")
                .customerEmail("a@b.com")
                .cardNumber("123")
                .build()))
                .isInstanceOf(InvalidCheckoutException.class);
    }
}
