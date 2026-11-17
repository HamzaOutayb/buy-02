package com.orderService.orderService.events;

public record OrderItemEvent(
    String orderId,
    String productId,
    Integer quantity,
    String operation
) {
    public OrderItemEvent(String productId, Integer quantity) {
        this(null, productId, quantity, "DECREASE");
    }
}