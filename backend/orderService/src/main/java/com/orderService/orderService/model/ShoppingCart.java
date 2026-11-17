package com.orderService.orderService.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("cart")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingCart {
    @Id
    private String userId;
    private List<CartItem> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CartItem {
        private String productId;
        private String productName;
        private double price;
        private int quantity;
        private String imageUrl;
        private String sellerId;
        private int maxQuantity;
    }
}
