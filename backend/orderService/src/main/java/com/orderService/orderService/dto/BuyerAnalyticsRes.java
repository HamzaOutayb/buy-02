package com.orderService.orderService.dto;

import java.util.List;

public record BuyerAnalyticsRes(
        double totalSpent,
        List<BuyerProductAnalyticsRes> mostBoughtProducts) {
}