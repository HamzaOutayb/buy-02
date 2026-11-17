package com.orderService.orderService.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.orderService.orderService.dto.CreateOrderReq;
import com.orderService.orderService.dto.OrderRes;
import com.orderService.orderService.dto.OrderSummaryRes;
import com.orderService.orderService.dto.ProductInfo;
import com.orderService.orderService.dto.SellerSalesAnalyticsRes;
import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.enums.PaymentMethod;
import com.orderService.orderService.enums.PaymentStatus;
import com.orderService.orderService.events.OrderEvent;
import com.orderService.orderService.model.DeliveryAddress;
import com.orderService.orderService.model.Order;
import com.orderService.orderService.model.OrderItem;
import com.orderService.orderService.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testCreateOrder_success() {
        when(authentication.getName()).thenReturn("customer-1");
        ProductInfo product = new ProductInfo("product-1", "Test Product", 100.0, 10, "seller-1", new String[]{"img1.jpg"});
        when(productClient.getProduct("product-1")).thenReturn(product);

        Order saved = order("order-1", "customer-1", OrderStatus.PENDING);
        saved.setItems(List.of(item("product-1", "seller-1")));
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        CreateOrderReq req = new CreateOrderReq(
            List.of(new CreateOrderReq.OrderItemReq("product-1", 2)),
            new CreateOrderReq.DeliveryAddressReq("John", "0600000000", "Rabat", "123 Main St"),
            PaymentMethod.CASH_ON_DELIVERY
        );

        OrderRes result = orderService.createOrder(req, authentication);

        assertNotNull(result);
        assertEquals("ORD-1", result.orderReference());
        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, result.paymentMethod());
        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedOrder.capture());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, savedOrder.getValue().getPaymentMethod());
    }

    @Test
    void testCreateOrder_insufficientStock() {
        when(authentication.getName()).thenReturn("customer-1");
        ProductInfo product = new ProductInfo("product-1", "Test Product", 100.0, 1, "seller-1", new String[]{"img1.jpg"});
        when(productClient.getProduct("product-1")).thenReturn(product);

        CreateOrderReq req = new CreateOrderReq(
            List.of(new CreateOrderReq.OrderItemReq("product-1", 5)),
            new CreateOrderReq.DeliveryAddressReq("John", "0600000000", "Rabat", "123 Main St"),
            PaymentMethod.CASH_ON_DELIVERY
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(req, authentication));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_productNotFound() {
        when(authentication.getName()).thenReturn("customer-1");
        when(productClient.getProduct("missing-product"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found: missing-product"));

        CreateOrderReq req = new CreateOrderReq(
            List.of(new CreateOrderReq.OrderItemReq("missing-product", 1)),
            new CreateOrderReq.DeliveryAddressReq("John", "0600000000", "Rabat", "123 Main St"),
            PaymentMethod.CASH_ON_DELIVERY
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(req, authentication));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCancelOrder_success() {
        Order order = order("order-1", "customer-1", OrderStatus.PENDING);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderRes result = orderService.cancelMyOrder("order-1", "customer-1");

        assertEquals(OrderStatus.CANCELLED, result.status());
    }

    @Test
    void testCancelOrder_wrongStatus() {
        Order order = order("order-1", "customer-1", OrderStatus.SHIPPED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelMyOrder("order-1", "customer-1"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testCancelOrder_notOwner() {
        Order order = order("order-1", "customer-1", OrderStatus.PENDING);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.cancelMyOrder("order-1", "other-customer"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testReorder_success() {
        Order original = order("order-1", "customer-1", OrderStatus.DELIVERED);
        original.setItems(List.of(item("product-1", "seller-1")));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(original));

        ProductInfo product = new ProductInfo("product-1", "Test Product", 100.0, 10, "seller-1", new String[]{"img1.jpg"});
        when(productClient.getProduct("product-1")).thenReturn(product);

        Order newOrder = order("order-2", "customer-1", OrderStatus.PENDING);
        newOrder.setItems(List.of(item("product-1", "seller-1")));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        OrderRes result = orderService.reorder("order-1", "customer-1");

        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.status());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testGetMyOrders_success() {
        Order order = order("order-1", "customer-1", OrderStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 20);

        when(mongoTemplate.count(any(Query.class), eq(Order.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Order.class))).thenReturn(List.of(order));

        Page<OrderSummaryRes> result = orderService.getMyOrders("customer-1", null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("ORD-1", result.getContent().get(0).orderReference());
    }

    @Test
    void testGetSellerAnalytics_countsOnlyDeliveredSellerItemsAsSales() {
        Order delivered = order("delivered", "customer-1", OrderStatus.DELIVERED);
        delivered.setItems(List.of(
                new OrderItem("p1", "seller-1", "Product 1", "", 25.0, 2, 50.0),
                new OrderItem("p2", "seller-2", "Other seller product", "", 40.0, 1, 40.0)));
        Order pending = order("pending", "customer-2", OrderStatus.PENDING);
        pending.setItems(List.of(item("p3", "seller-1")));
        when(mongoTemplate.find(any(Query.class), eq(Order.class))).thenReturn(List.of(delivered, pending));

        SellerSalesAnalyticsRes result = orderService.getSellerAnalytics("seller-1");

        assertEquals(2, result.totalOrders());
        assertEquals(1, result.pendingOrders());
        assertEquals(1, result.completedOrders());
        assertEquals(2, result.unitsSold());
        assertEquals(50.0, result.revenue());
    }

    @Test
    void testSellerConfirmOrder_success() {
        Order order = order("order-1", "customer-1", OrderStatus.PENDING);
        order.setItems(List.of(item("product-1", "seller-1")));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderRes result = orderService.confirmOrder("order-1", "seller-1");

        assertEquals(OrderStatus.CONFIRMED, result.status());
    }

    @Test
    void testSellerConfirmOrder_forbidden() {
        Order order = order("order-1", "customer-1", OrderStatus.PENDING);
        order.setItems(List.of(item("product-1", "seller-1")));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.confirmOrder("order-1", "other-seller"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testSellerUpdateStatus_validTransition() {
        Order order = order("order-1", "customer-1", OrderStatus.CONFIRMED);
        order.setItems(List.of(item("product-1", "seller-1")));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderRes result = orderService.updateSellerOrderStatus("order-1", "seller-1", OrderStatus.PROCESSING);

        assertEquals(OrderStatus.PROCESSING, result.status());
    }

    @Test
    void testSellerUpdateStatus_invalidTransition() {
        Order order = order("order-1", "customer-1", OrderStatus.PENDING);
        order.setItems(List.of(item("product-1", "seller-1")));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.updateSellerOrderStatus("order-1", "seller-1", OrderStatus.DELIVERED));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testSellerCancelOrder_success() {
        Order order = order("order-1", "customer-1", OrderStatus.CONFIRMED);
        order.setItems(List.of(item("product-1", "seller-1")));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderRes result = orderService.cancelSellerOrder("order-1", "seller-1");

        assertEquals(OrderStatus.CANCELLED, result.status());
    }

    @Test
    void testGetSellerOrderById_isolatesOnlySellerItems() {
        Order order = order("order-1", "customer-1", OrderStatus.CONFIRMED);
        OrderItem sellerItem = item("product-1", "seller-1");
        OrderItem otherItem = item("product-2", "seller-2");
        order.setItems(List.of(sellerItem, otherItem));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        OrderRes result = orderService.getSellerOrderById("order-1", "seller-1");

        assertEquals(1, result.items().size());
        assertEquals("seller-1", result.items().get(0).sellerId());
        assertEquals(100.0, result.total());
    }

    // Helpers
    private Order order(String id, String customerId, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setOrderReference("ORD-1");
        o.setCustomerId(customerId);
        o.setStatus(status);
        o.setItems(List.of(item("p1", "s1")));
        o.setDeliveryAddress(new DeliveryAddress("John", "0600000000", "Rabat", "123 Main St"));
        o.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setSubtotal(100.0);
        o.setShippingFee(0.0);
        o.setTotal(100.0);
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return o;
    }

    private OrderItem item(String productId, String sellerId) {
        return new OrderItem(productId, sellerId, "Test Product", "img.jpg", 100.0, 1, 100.0);
    }
}
