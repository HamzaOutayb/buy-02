package com.example.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductSummary(
        String id,
        String name,
        String description,
        String category,
        BigDecimal price,
        Integer quantity,
        String sellerId,
        List<String> imageUrls) {
}
