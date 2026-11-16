package com.orderService.orderService.events;

import java.time.Instant;

public record OrderEvent(
    String eventType,
    String orderId,
    String orderReference,
    String customerId,
    Instant timestamp
) {}
