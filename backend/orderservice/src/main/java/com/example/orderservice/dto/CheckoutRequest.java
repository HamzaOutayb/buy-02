package com.example.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CheckoutRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{8,80}") String checkoutKey,
        @NotNull @Valid DeliveryAddressRequest deliveryAddress) {
}
