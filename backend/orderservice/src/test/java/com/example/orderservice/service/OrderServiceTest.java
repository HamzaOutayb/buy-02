package com.example.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.mockito.invocation.InvocationOnMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.dto.CheckoutRequest;
import com.example.orderservice.dto.DeliveryAddressRequest;
import com.example.orderservice.dto.ProductSummary;
import com.example.orderservice.exception.ApiException;
import com.example.orderservice.model.Cart;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductClient productClient;
    @Mock
    private MongoTemplate mongoTemplate;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(cartRepository, orderRepository, productClient, mongoTemplate);
    }

    @Test
    void duplicateCheckoutReturnsExistingOrderWithoutReleasingItsInventoryReservation() {
        String buyerId = "buyer-1";
        String checkoutKey = "checkout-key-123";
        Cart cart = new Cart();
        cart.setBuyerId(buyerId);
        cart.setItems(new ArrayList<>(List.of(new Cart.CartLine("product-1", 2))));
        when(orderRepository.findByBuyerIdAndCheckoutKey(buyerId, checkoutKey))
                .thenReturn(List.of(), List.of(existingOrder()));
        when(cartRepository.findByBuyerId(buyerId)).thenReturn(Optional.of(cart));
        when(productClient.reserve(eq("product-1"), eq(2), any()))
                .thenReturn(new ProductSummary(
                        "product-1", "Product", null, "general", BigDecimal.TEN, 8, "seller-1", List.of()));
        when(orderRepository.save(any(Order.class))).thenThrow(new DuplicateKeyException("duplicate checkout"));

        List<?> result = orderService.checkout(
                buyerId,
                new CheckoutRequest(checkoutKey, new DeliveryAddressRequest(
                        "Buyer", "123456789", "Street", "City", "10000")),
                "Bearer token");

        assertThat(result).hasSize(1);
        verify(productClient, never()).release(any(), any());
        verify(cartRepository, never()).deleteByBuyerId(buyerId);
    }

    @Test
    void checkoutSnapshotsCurrentProductAndClearsCart() {
        String buyerId = "buyer-1";
        String checkoutKey = "checkout-key-123";
        Cart cart = new Cart();
        cart.setBuyerId(buyerId);
        cart.setItems(new ArrayList<>(List.of(new Cart.CartLine("product-1", 2))));
        when(orderRepository.findByBuyerIdAndCheckoutKey(buyerId, checkoutKey)).thenReturn(List.of());
        when(cartRepository.findByBuyerId(buyerId)).thenReturn(Optional.of(cart));
        when(productClient.reserve(eq("product-1"), eq(2), any()))
                .thenReturn(new ProductSummary(
                        "product-1", "Product", null, "general", BigDecimal.TEN, 8, "seller-1", List.of()));
        when(orderRepository.save(any(Order.class))).thenAnswer(this::saveWithId);

        var result = orderService.checkout(
                buyerId,
                new CheckoutRequest(checkoutKey, new DeliveryAddressRequest(
                        "Buyer", "123456789", "Street", "City", "10000")),
                "Bearer token");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("order-1");
        assertThat(result.get(0).items()).hasSize(1);
        assertThat(result.get(0).items().get(0).unitPrice()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(result.get(0).total()).isEqualByComparingTo(BigDecimal.valueOf(20));
        verify(cartRepository).deleteByBuyerId(buyerId);
        verify(productClient, never()).release(any(), any());
    }

    @Test
    void buyerOrderSearchRejectsUnknownStatus() {
        assertThatThrownBy(() -> orderService.buyerOrders("buyer-1", "", "UNKNOWN", "", ""))
                .isInstanceOf(ApiException.class);
        verify(mongoTemplate, never()).find(any(org.springframework.data.mongodb.core.query.Query.class), eq(Order.class));
    }

    @Test
    void buyerCannotReadAnotherBuyersOrder() {
        Order order = existingOrder();
        when(orderRepository.findById("existing-order")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder("existing-order", "buyer-2"))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(productClient);
    }

    private Order saveWithId(InvocationOnMock invocation) {
        Order saved = invocation.getArgument(0);
        saved.setId("order-1");
        return saved;
    }

    private Order existingOrder() {
        Order order = new Order();
        order.setId("existing-order");
        order.setBuyerId("buyer-1");
        order.setSellerId("seller-1");
        order.setItems(List.of());
        order.setDeliveryAddress(new Order.DeliveryAddress("Buyer", "123456789", "Street", "City", "10000"));
        order.setTotal(BigDecimal.valueOf(20));
        order.setStatus(OrderStatus.PLACED);
        return order;
    }
}
