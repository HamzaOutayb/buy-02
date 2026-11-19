package com.orderService.orderService.dto;

public record BuyerProductAnalyticsRes(
        String productId,
        String productName,
        long quantity,
        double spent) {
}