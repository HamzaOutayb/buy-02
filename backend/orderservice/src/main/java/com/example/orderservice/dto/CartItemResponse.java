package com.example.orderservice.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        String productId,
        String name,
        String category,
        String sellerId,
        java.util.List<String> imageUrls,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
