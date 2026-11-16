package com.orderService.orderService.dto;

public record DeliveryAddressRes(
    String recipientName,
    String phone,
    String city,
    String fullAddress
) {}
