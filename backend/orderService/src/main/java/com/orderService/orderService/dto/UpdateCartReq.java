package com.orderService.orderService.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCartReq(
        @NotNull List<@Valid CartItemReq> items) {
    public record CartItemReq(
            @NotBlank String productId,
            @NotNull @Min(1) Integer quantity) {
    }
}
