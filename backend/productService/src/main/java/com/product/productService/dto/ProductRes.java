package com.product.productService.dto;

public record ProductRes(String id, String name, String description, Double price, Integer quantity, String userId,
                String[] imageUrls, String[] categories) {

        public ProductRes(String id, String name, String description, Double price, Integer quantity, String userId,
                        String[] imageUrls) {
                this(id, name, description, price, quantity, userId, imageUrls, new String[0]);
        }
}