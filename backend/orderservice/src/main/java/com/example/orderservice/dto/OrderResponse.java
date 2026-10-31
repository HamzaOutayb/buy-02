package com.example.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;

public record OrderResponse(
        String id,
        String buyerId,
        String sellerId,
        List<OrderItemResponse> items,
        Order.DeliveryAddress deliveryAddress,
        BigDecimal total,
        String paymentMethod,
        OrderStatus status,
        Instant createdAt) {

    public record OrderItemResponse(
            String productId,
            String productName,
            String category,
            String sellerId,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal) {
    }
}
