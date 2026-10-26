package com.example.productservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String sellerId;
    private List<String> imageUrls;
    private Instant createdAt;
    private Instant updatedAt;
}
