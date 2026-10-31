package com.example.orderservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document("orders")
@CompoundIndex(
        name = "buyer_checkout_seller_unique",
        def = "{'buyerId': 1, 'checkoutKey': 1, 'sellerId': 1}",
        unique = true)
public class Order {
    @Id
    private String id;

    @Indexed
    private String buyerId;

    @Indexed
    private String sellerId;

    private List<OrderItem> items;
    private DeliveryAddress deliveryAddress;
    private BigDecimal total;
    private String paymentMethod = "PAY_ON_DELIVERY";
    private OrderStatus status = OrderStatus.PLACED;
    private String checkoutKey;
    private boolean archived;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @Data
    @NoArgsConstructor
    public static class OrderItem {
        private String productId;
        private String productName;
        private String category;
        private String sellerId;
        private String reservationId;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal lineTotal;

        public OrderItem(
                String productId,
                String productName,
                String category,
                String sellerId,
                String reservationId,
                BigDecimal unitPrice,
                int quantity,
                BigDecimal lineTotal) {
            this.productId = productId;
            this.productName = productName;
            this.category = category;
            this.sellerId = sellerId;
            this.reservationId = reservationId;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.lineTotal = lineTotal;
        }
    }

    @Data
    @NoArgsConstructor
    public static class DeliveryAddress {
        private String recipientName;
        private String phone;
        private String street;
        private String city;
        private String postalCode;

        public DeliveryAddress(String recipientName, String phone, String street, String city, String postalCode) {
            this.recipientName = recipientName;
            this.phone = phone;
            this.street = street;
            this.city = city;
            this.postalCode = postalCode;
        }
    }
}
