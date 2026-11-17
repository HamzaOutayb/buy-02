package com.product.productService.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.product.productService.config.InventoryTopic;
import com.product.productService.dto.OrderItemEvent;
import com.product.productService.model.Product;
import com.product.productService.reposiroty.ProductRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class InventoryEventConsumer {
    private final ProductRepository productRepository;

    @KafkaListener(topics = InventoryTopic.NAME, groupId = "product-service-inventory")
    public void consume(OrderItemEvent event) {
        Product product = productRepository.findById(event.productId())
                .orElseThrow(() -> new IllegalStateException("Product not found: " + event.productId()));
        int currentQuantity = product.getQuantity() == null ? 0 : product.getQuantity();
        int orderedQuantity = event.quantity() == null ? 0 : event.quantity();
        if (orderedQuantity <= 0) {
            throw new IllegalStateException("Invalid quantity for product: " + event.productId());
        }
        if ("INCREASE".equals(event.operation())) {
            product.setQuantity(currentQuantity + orderedQuantity);
        } else {
            if (currentQuantity < orderedQuantity) {
                throw new IllegalStateException("Insufficient stock for product: " + event.productId());
            }
            product.setQuantity(currentQuantity - orderedQuantity);
        }
        productRepository.save(product);
    }
}