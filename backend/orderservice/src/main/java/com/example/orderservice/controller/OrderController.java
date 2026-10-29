package com.example.orderservice.controller;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.orderservice.dto.CheckoutRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public List<OrderResponse> checkout(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(authentication.getName(), request, token);
    }

    @GetMapping
    public List<OrderResponse> buyerOrders(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return orderService.buyerOrders(authentication.getName(), q, status, from, to);
    }

    @GetMapping("/{orderId}")
    public OrderResponse orderDetails(@PathVariable String orderId, Authentication authentication) {
        return orderService.getOrder(orderId, authentication.getName());
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(
            @PathVariable String orderId,
            Authentication authentication) {
        return orderService.cancelOrder(orderId, authentication.getName());
    }

    @DeleteMapping("/{orderId}")
    public void archive(@PathVariable String orderId, Authentication authentication) {
        orderService.archiveOrder(orderId, authentication.getName());
    }

    @PostMapping("/{orderId}/reorder")
    public void reorder(
            @PathVariable String orderId,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        orderService.reorder(orderId, authentication.getName(), token);
    }
}
