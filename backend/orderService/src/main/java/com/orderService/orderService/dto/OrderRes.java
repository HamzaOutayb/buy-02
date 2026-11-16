package com.orderService.orderService.dto;

import java.time.Instant;
import java.util.List;

import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.enums.PaymentMethod;
import com.orderService.orderService.enums.PaymentStatus;

public record OrderRes(
    String id,
    String orderReference,
    String customerId,
    OrderStatus status,
    List<OrderItemRes> items,
    DeliveryAddressRes deliveryAddress,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    Double subtotal,
    Double shippingFee,
    Double total,
    Instant createdAt,
    Instant updatedAt
) {}
