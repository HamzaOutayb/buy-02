package com.orderService.orderService.events;

public record OrderItemEvent(
    String orderId,
    String productId,
    Integer quantity
) {
    public OrderItemEvent(String productId, Integer quantity) {
        this(null, productId, quantity);
    }
}