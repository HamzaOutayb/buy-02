package com.product.productService.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.product.productService.dto.OrderItemEvent;
import com.product.productService.model.Product;
import com.product.productService.reposiroty.ProductRepository;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTests {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryEventConsumer consumer;

    @Test
    void consumeDecreaseEventReducesProductQuantity() {
        Product product = product("product-1", 10);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

        consumer.consume(new OrderItemEvent("order-1", "product-1", 3, "DECREASE"));

        assertEquals(7, product.getQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void consumeIncreaseEventRefundsProductQuantity() {
        Product product = product("product-1", 7);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

        consumer.consume(new OrderItemEvent("order-1", "product-1", 3, "INCREASE"));

        assertEquals(10, product.getQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void consumeDecreaseEventRejectsInsufficientStock() {
        Product product = product("product-1", 2);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

        assertThrows(IllegalStateException.class,
                () -> consumer.consume(new OrderItemEvent("order-1", "product-1", 3, "DECREASE")));
    }

    private Product product(String id, int quantity) {
        return new Product(id, "Product", "Description", 10.0, quantity, "seller-1");
    }
}