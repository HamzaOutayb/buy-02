package com.orderService.orderService.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderReq(
    @NotNull @NotEmpty List<@Valid OrderItemReq> items,
    @NotNull @Valid DeliveryAddressReq deliveryAddress
) {
    public record OrderItemReq(
        @NotBlank String productId,
        @NotNull @Min(1) Integer quantity
    ) {}

    public record DeliveryAddressReq(
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 300) String fullAddress
    ) {}
}
