package com.product.productService.dto;

public record OrderItemEvent(
        String orderId,
        String productId,
        Integer quantity,
        String operation) {
}