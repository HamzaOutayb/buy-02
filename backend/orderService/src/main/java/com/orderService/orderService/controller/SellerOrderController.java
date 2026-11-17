package com.orderService.orderService.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orderService.orderService.dto.OrderRes;
import com.orderService.orderService.dto.OrderSummaryRes;
import com.orderService.orderService.dto.SellerSalesAnalyticsRes;
import com.orderService.orderService.dto.UpdateStatusReq;
import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.service.OrderService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders/seller")
@PreAuthorize("hasRole('SELLER')")
@AllArgsConstructor
public class SellerOrderController {

    private final OrderService orderService;

    @GetMapping("/analytics")
    public SellerSalesAnalyticsRes getSellerAnalytics(Authentication auth) {
        return orderService.getSellerAnalytics(auth.getName());
    }

    @GetMapping
    public Page<OrderSummaryRes> getSellerOrders(
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String orderReference,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.getSellerOrders(auth.getName(), customer, orderReference, status, dateFrom, dateTo, pageable);
    }

    @GetMapping("/{id}")
    public OrderRes getSellerOrderById(@PathVariable String id, Authentication auth) {
        return orderService.getSellerOrderById(id, auth.getName());
    }

    @PostMapping("/{id}/confirm")
    public OrderRes confirmOrder(@PathVariable String id, Authentication auth) {
        return orderService.confirmOrder(id, auth.getName());
    }

    @PutMapping("/{id}/status")
    public OrderRes updateSellerOrderStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateStatusReq req,
            Authentication auth) {
        return orderService.updateSellerOrderStatus(id, auth.getName(), req.status());
    }

    @PostMapping("/{id}/cancel")
    public OrderRes cancelSellerOrder(@PathVariable String id, Authentication auth) {
        return orderService.cancelSellerOrder(id, auth.getName());
    }
}
