package com.example.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StockReservationRequest(
        @Min(1) int quantity,
        @NotBlank String reservationId) {
}
