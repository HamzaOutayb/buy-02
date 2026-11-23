package com.orderService.orderService.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.orderService.orderService.dto.UpdateCartReq;
import com.orderService.orderService.model.ShoppingCart;
import com.orderService.orderService.service.CartService;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CartController controller;

    @Test
    void getCart_delegatesToService() {
        when(authentication.getName()).thenReturn("user-1");
        List<ShoppingCart.CartItem> items = List.of(new ShoppingCart.CartItem("p1", "Product 1", 10.0, 2, "img.jpg", "s1", 5));
        when(cartService.getCart("user-1")).thenReturn(items);

        List<ShoppingCart.CartItem> result = controller.getCart(authentication);

        assertEquals(items, result);
        verify(cartService).getCart("user-1");
    }

    @Test
    void updateCart_delegatesToService() {
        when(authentication.getName()).thenReturn("user-1");
        UpdateCartReq req = new UpdateCartReq(List.of(new UpdateCartReq.CartItemReq("p1", 3)));
        List<ShoppingCart.CartItem> items = List.of(new ShoppingCart.CartItem("p1", "Product 1", 10.0, 3, "img.jpg", "s1", 5));
        when(cartService.updateCart("user-1", req)).thenReturn(items);

        List<ShoppingCart.CartItem> result = controller.updateCart(req, authentication);

        assertEquals(items, result);
        verify(cartService).updateCart("user-1", req);
    }
}
