package com.example.orderservice.controller;

import java.util.List;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderStatusRequest;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/seller/orders")
public class SellerOrderController {
    private final OrderService orderService;

    public SellerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> sellerOrders(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return orderService.sellerOrders(authentication.getName(), q, status, from, to);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @PathVariable String orderId,
            Authentication authentication,
            @Valid @RequestBody OrderStatusRequest request) {
        return orderService.updateSellerStatus(orderId, authentication.getName(), request.status());
    }
}
