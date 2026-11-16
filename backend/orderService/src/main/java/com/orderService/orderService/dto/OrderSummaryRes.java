package com.orderService.orderService.dto;

import java.time.Instant;

import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.enums.PaymentMethod;

public record OrderSummaryRes(
    String id,
    String orderReference,
    String customerId,
    OrderStatus status,
    int itemCount,
    Double total,
    PaymentMethod paymentMethod,
    Instant createdAt
) {}
