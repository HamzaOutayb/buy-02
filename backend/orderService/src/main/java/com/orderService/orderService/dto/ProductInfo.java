package com.orderService.orderService.dto;

public record ProductInfo(
    String id,
    String name,
    Double price,
    Integer quantity,
    String userId,
    String[] imageUrls
) {}
