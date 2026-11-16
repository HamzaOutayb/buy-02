package com.orderService.orderService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryAddress {
    private String recipientName;
    private String phone;
    private String city;
    private String fullAddress;
}
