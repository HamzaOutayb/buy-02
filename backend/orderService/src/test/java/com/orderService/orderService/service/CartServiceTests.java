package com.orderService.orderService.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orderService.orderService.dto.ProductInfo;
import com.orderService.orderService.dto.UpdateCartReq;
import com.orderService.orderService.model.ShoppingCart;
import com.orderService.orderService.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTests {
    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartService cartService;

    @Test
    void getCart_returnsOnlyTheRequestedUsersCart() {
        List<ShoppingCart.CartItem> items = List.of(
                new ShoppingCart.CartItem("product-1", "Product", 10.0, 2, "image.jpg", "seller-1", 5));
        when(cartRepository.findById("customer-1"))
                .thenReturn(Optional.of(new ShoppingCart("customer-1", items)));

        assertEquals(items, cartService.getCart("customer-1"));
    }

    @Test
    void updateCartStoresProductDetailsUnderTheAuthenticatedUserId() {
        ProductInfo product = new ProductInfo(
                "product-1", "Product", 10.0, 5, "seller-1", new String[]{"image.jpg"});
        when(productClient.getProduct("product-1")).thenReturn(product);
        when(cartRepository.save(org.mockito.ArgumentMatchers.any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UpdateCartReq req = new UpdateCartReq(List.of(new UpdateCartReq.CartItemReq("product-1", 2)));

        List<ShoppingCart.CartItem> result = cartService.updateCart("customer-1", req);

        assertEquals(List.of(
                new ShoppingCart.CartItem("product-1", "Product", 10.0, 2, "image.jpg", "seller-1", 5)),
                result);
        verify(cartRepository).save(new ShoppingCart("customer-1", result));
    }
}
