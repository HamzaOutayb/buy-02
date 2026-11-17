package com.orderService.orderService.dto;

public record SellerSalesAnalyticsRes(
        long totalOrders,
        long pendingOrders,
        long completedOrders,
        long unitsSold,
        double revenue) {
}
