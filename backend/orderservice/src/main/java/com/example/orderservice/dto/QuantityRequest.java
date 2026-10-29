package com.example.orderservice.dto;

import jakarta.validation.constraints.Min;

public record QuantityRequest(@Min(1) int quantity) {
}
