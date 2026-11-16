package com.orderService.orderService.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orderService.orderService.dto.CreateOrderReq;
import com.orderService.orderService.dto.OrderRes;
import com.orderService.orderService.dto.OrderSummaryRes;
import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.service.OrderService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CLIENT')")
@AllArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderRes> createOrder(@RequestBody @Valid CreateOrderReq req, Authentication auth) {
        OrderRes result = orderService.createOrder(req, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public Page<OrderSummaryRes> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) String orderReference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.getMyOrders(auth.getName(), status, dateFrom, dateTo, orderReference, pageable);
    }

    @GetMapping("/{id}")
    public OrderRes getMyOrderById(@PathVariable String id, Authentication auth) {
        return orderService.getMyOrderById(id, auth.getName());
    }

    @PostMapping("/{id}/cancel")
    public OrderRes cancelMyOrder(@PathVariable String id, Authentication auth) {
        return orderService.cancelMyOrder(id, auth.getName());
    }

    @PostMapping("/{id}/reorder")
    public ResponseEntity<OrderRes> reorder(@PathVariable String id, Authentication auth) {
        OrderRes result = orderService.reorder(id, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
