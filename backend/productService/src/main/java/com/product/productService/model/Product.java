package com.product.productService.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("product")
@AllArgsConstructor
@Data
@NoArgsConstructor
public class Product {
    @Id
    String id;
    String name;
    String description;
    Double price;
    Integer quantity;
    String userId;
    String[] categories;

    public Product(String id, String name, String description, Double price, Integer quantity, String userId) {
        this(id, name, description, price, quantity, userId, new String[0]);
    }
}