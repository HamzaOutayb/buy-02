package com.orderService.orderService.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderService.orderService.dto.UpdateCartReq;
import com.orderService.orderService.model.ShoppingCart;
import com.orderService.orderService.service.CartService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CLIENT')")
@AllArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public List<ShoppingCart.CartItem> getCart(Authentication auth) {
        return cartService.getCart(auth.getName());
    }

    @PutMapping
    public List<ShoppingCart.CartItem> updateCart(
            @RequestBody @Valid UpdateCartReq req,
            Authentication auth) {
        return cartService.updateCart(auth.getName(), req);
    }
}
