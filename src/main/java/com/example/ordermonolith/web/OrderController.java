package com.example.ordermonolith.web;

import com.example.ordermonolith.persistence.entity.Order;
import com.example.ordermonolith.service.CheckoutCommand;
import com.example.ordermonolith.service.OrderService;
import com.example.ordermonolith.web.dto.CheckoutRequest;
import com.example.ordermonolith.web.dto.CheckoutResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for checkout. Deliberately tiny: bind + validate the request,
 * translate it to a {@link CheckoutCommand}, delegate, map the result. No
 * {@code try/catch} &ndash; failures are domain exceptions handled by
 * {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        Order order = orderService.checkout(toCommand(request));
        return CheckoutResponse.from(order);
    }

    private static CheckoutCommand toCommand(CheckoutRequest request) {
        var items = request.items().stream()
                .map(item -> CheckoutCommand.Item.builder()
                        .productId(item.productId())
                        .quantity(item.quantity())
                        .build())
                .toList();
        return CheckoutCommand.builder()
                .customerEmail(request.customerEmail())
                .items(items)
                .paymentMethod(request.paymentMethod())
                .coupon(request.coupon())
                .cardNumber(request.cardNumber())
                .build();
    }
}
