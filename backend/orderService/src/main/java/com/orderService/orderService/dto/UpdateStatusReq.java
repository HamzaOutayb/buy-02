package com.orderService.orderService.dto;

import com.orderService.orderService.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusReq(
    @NotNull OrderStatus status
) {}
