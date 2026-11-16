package com.orderService.orderService.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.orderService.orderService.dto.CreateOrderReq;
import com.orderService.orderService.dto.DeliveryAddressRes;
import com.orderService.orderService.dto.OrderRes;
import com.orderService.orderService.dto.OrderSummaryRes;
import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.enums.PaymentMethod;
import com.orderService.orderService.enums.PaymentStatus;
import com.orderService.orderService.service.OrderService;

@ExtendWith(MockitoExtension.class)
class CustomerOrderControllerTests {

    @Mock
    private OrderService orderService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CustomerOrderController controller;

    @Test
    void createOrder_returnsCreated() {
        CreateOrderReq req = new CreateOrderReq(
            List.of(new CreateOrderReq.OrderItemReq("p1", 2)),
            new CreateOrderReq.DeliveryAddressReq("Jane", "0612345678", "Casablanca", "Bd Zerktouni")
        );
        OrderRes expected = orderRes();
        when(orderService.createOrder(req, authentication)).thenReturn(expected);

        ResponseEntity<OrderRes> response = controller.createOrder(req, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(orderService).createOrder(req, authentication);
    }

    @Test
    void getMyOrders_delegatesToService() {
        when(authentication.getName()).thenReturn("cust-1");
        Page<OrderSummaryRes> page = new PageImpl<>(List.of(
            new OrderSummaryRes("o1", "REF1", "cust-1", OrderStatus.PENDING, 2, 200.0, PaymentMethod.CASH_ON_DELIVERY, Instant.now())
        ));
        when(orderService.getMyOrders(eq("cust-1"), eq(OrderStatus.PENDING), any(), any(), eq("REF1"), any()))
                .thenReturn(page);

        Page<OrderSummaryRes> res = controller.getMyOrders(OrderStatus.PENDING, null, null, "REF1", 0, 20, authentication);

        assertEquals(1, res.getTotalElements());
        verify(orderService).getMyOrders(eq("cust-1"), eq(OrderStatus.PENDING), any(), any(), eq("REF1"), any());
    }

    @Test
    void getMyOrderById_delegatesToService() {
        when(authentication.getName()).thenReturn("cust-1");
        OrderRes expected = orderRes();
        when(orderService.getMyOrderById("o1", "cust-1")).thenReturn(expected);

        OrderRes res = controller.getMyOrderById("o1", authentication);

        assertEquals(expected, res);
        verify(orderService).getMyOrderById("o1", "cust-1");
    }

    @Test
    void cancelMyOrder_delegatesToService() {
        when(authentication.getName()).thenReturn("cust-1");
        OrderRes expected = orderRes();
        when(orderService.cancelMyOrder("o1", "cust-1")).thenReturn(expected);

        OrderRes res = controller.cancelMyOrder("o1", authentication);

        assertEquals(expected, res);
        verify(orderService).cancelMyOrder("o1", "cust-1");
    }

    @Test
    void reorder_returnsCreated() {
        when(authentication.getName()).thenReturn("cust-1");
        OrderRes expected = orderRes();
        when(orderService.reorder("o1", "cust-1")).thenReturn(expected);

        ResponseEntity<OrderRes> response = controller.reorder("o1", authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(orderService).reorder("o1", "cust-1");
    }

    private OrderRes orderRes() {
        return new OrderRes("o1", "REF1", "cust-1", OrderStatus.PENDING, List.of(),
                new DeliveryAddressRes("Jane", "0612345678", "Casablanca", "Bd Zerktouni"),
                PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING, 100.0, 0.0, 100.0,
                Instant.now(), Instant.now());
    }
}
