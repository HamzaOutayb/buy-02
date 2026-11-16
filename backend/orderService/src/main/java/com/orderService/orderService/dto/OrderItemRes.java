package com.orderService.orderService.dto;

public record OrderItemRes(
    String productId,
    String sellerId,
    String productName,
    String imageUrl,
    Double unitPrice,
    Integer quantity,
    Double subtotal
) {}
