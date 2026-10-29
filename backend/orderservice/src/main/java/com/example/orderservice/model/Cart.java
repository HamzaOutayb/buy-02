package com.example.orderservice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document("carts")
public class Cart {
    @Id
    private String id;

    @Indexed(unique = true)
    private String buyerId;

    private List<CartLine> items = new ArrayList<>();
    private Instant updatedAt = Instant.now();

    @Data
    @NoArgsConstructor
    public static class CartLine {
        private String productId;
        private int quantity;

        public CartLine(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}
