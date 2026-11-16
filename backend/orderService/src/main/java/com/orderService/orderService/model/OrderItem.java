package com.orderService.orderService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    private String productId;
    private String sellerId;
    private String productName;
    private String imageUrl;
    private Double unitPrice;
    private Integer quantity;
    private Double subtotal;
}
