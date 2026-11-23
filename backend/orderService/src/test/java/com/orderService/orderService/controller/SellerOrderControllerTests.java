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
import org.springframework.security.core.Authentication;

import com.orderService.orderService.dto.DeliveryAddressRes;
import com.orderService.orderService.dto.OrderRes;
import com.orderService.orderService.dto.OrderSummaryRes;
import com.orderService.orderService.dto.SellerSalesAnalyticsRes;
import com.orderService.orderService.dto.UpdateStatusReq;
import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.enums.PaymentMethod;
import com.orderService.orderService.enums.PaymentStatus;
import com.orderService.orderService.service.OrderService;

@ExtendWith(MockitoExtension.class)
class SellerOrderControllerTests {

    @Mock
    private OrderService orderService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SellerOrderController controller;

    @Test
    void getSellerAnalytics_delegatesToService() {
        when(authentication.getName()).thenReturn("seller-1");
        SellerSalesAnalyticsRes expected = new SellerSalesAnalyticsRes(10, 5, 5, 20, 200.0);
        when(orderService.getSellerAnalytics("seller-1")).thenReturn(expected);

        SellerSalesAnalyticsRes res = controller.getSellerAnalytics(authentication);

        assertEquals(expected, res);
        verify(orderService).getSellerAnalytics("seller-1");
    }

    @Test
    void getSellerOrders_delegatesToService() {
        when(authentication.getName()).thenReturn("seller-1");
        Page<OrderSummaryRes> page = new PageImpl<>(List.of(
            new OrderSummaryRes("o1", "REF1", "cust-1", OrderStatus.PENDING, 1, 50.0, PaymentMethod.CASH_ON_DELIVERY, Instant.now())
        ));
        when(orderService.getSellerOrders(eq("seller-1"), eq("cust-1"), eq("REF1"), eq(OrderStatus.PENDING), any(), any(), any()))
                .thenReturn(page);

        Page<OrderSummaryRes> res = controller.getSellerOrders("cust-1", "REF1", OrderStatus.PENDING, null, null, 0, 20, authentication);

        assertEquals(1, res.getTotalElements());
        verify(orderService).getSellerOrders(eq("seller-1"), eq("cust-1"), eq("REF1"), eq(OrderStatus.PENDING), any(), any(), any());
    }

    @Test
    void getSellerOrderById_delegatesToService() {
        when(authentication.getName()).thenReturn("seller-1");
        OrderRes expected = orderRes();
        when(orderService.getSellerOrderById("o1", "seller-1")).thenReturn(expected);

        OrderRes res = controller.getSellerOrderById("o1", authentication);

        assertEquals(expected, res);
        verify(orderService).getSellerOrderById("o1", "seller-1");
    }

    @Test
    void confirmOrder_delegatesToService() {
        when(authentication.getName()).thenReturn("seller-1");
        OrderRes expected = orderRes();
        when(orderService.confirmOrder("o1", "seller-1")).thenReturn(expected);

        OrderRes res = controller.confirmOrder("o1", authentication);

        assertEquals(expected, res);
        verify(orderService).confirmOrder("o1", "seller-1");
    }

    @Test
    void updateSellerOrderStatus_delegatesToService() {
        when(authentication.getName()).thenReturn("seller-1");
        OrderRes expected = orderRes();
        when(orderService.updateSellerOrderStatus("o1", "seller-1", OrderStatus.PROCESSING)).thenReturn(expected);

        OrderRes res = controller.updateSellerOrderStatus("o1", new UpdateStatusReq(OrderStatus.PROCESSING), authentication);

        assertEquals(expected, res);
        verify(orderService).updateSellerOrderStatus("o1", "seller-1", OrderStatus.PROCESSING);
    }

    @Test
    void cancelSellerOrder_delegatesToService() {
        when(authentication.getName()).thenReturn("seller-1");
        OrderRes expected = orderRes();
        when(orderService.cancelSellerOrder("o1", "seller-1")).thenReturn(expected);

        OrderRes res = controller.cancelSellerOrder("o1", authentication);

        assertEquals(expected, res);
        verify(orderService).cancelSellerOrder("o1", "seller-1");
    }

    private OrderRes orderRes() {
        return new OrderRes("o1", "REF1", "cust-1", OrderStatus.PENDING, List.of(),
                new DeliveryAddressRes("Jane", "0612345678", "Casablanca", "Bd Zerktouni"),
                PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING, 50.0, 0.0, 50.0,
                Instant.now(), Instant.now());
    }
}
