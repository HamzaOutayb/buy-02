package com.example.orderservice.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.orderservice.dto.CartItemRequest;
import com.example.orderservice.dto.CartResponse;
import com.example.orderservice.dto.QuantityRequest;
import com.example.orderservice.service.CartService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(Authentication authentication, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return cartService.getCart(authentication.getName(), token);
    }

    @PostMapping("/items")
    public CartResponse addItem(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(authentication.getName(), request, token);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable String productId,
            @Valid @RequestBody QuantityRequest request) {
        return cartService.updateItem(authentication.getName(), productId, request.quantity(), token);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @PathVariable String productId) {
        return cartService.removeItem(authentication.getName(), productId, token);
    }
}
