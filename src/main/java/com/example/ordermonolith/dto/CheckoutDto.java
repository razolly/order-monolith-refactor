package com.example.ordermonolith.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CheckoutDto {

    @NotBlank(message = "customerEmail is required")
    @Email(message = "customerEmail is not a valid email")
    private String customerEmail;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<CheckoutItemDto> items;

    @NotBlank(message = "paymentMethod is required")
    @Pattern(regexp = "STRIPE|PAYPAL|CREDIT_CARD", message = "unsupported paymentMethod")
    private String paymentMethod;

    /** Optional coupon code, e.g. "SAVE5". */
    private String coupon;

    /** Only used (and only required) when paymentMethod is CREDIT_CARD. */
    private String cardNumber;
}
