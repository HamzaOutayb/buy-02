package com.orderService.orderService.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orderService.orderService.dto.ProductInfo;
import com.orderService.orderService.dto.UpdateCartReq;
import com.orderService.orderService.model.ShoppingCart;
import com.orderService.orderService.repository.CartRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public List<ShoppingCart.CartItem> getCart(String userId) {
        return cartRepository.findById(userId)
                .map(ShoppingCart::getItems)
                .orElseGet(List::of);
    }

    public List<ShoppingCart.CartItem> updateCart(String userId, UpdateCartReq req) {
        List<ShoppingCart.CartItem> items = req.items().stream()
                .map(item -> toCartItem(item.productId(), item.quantity()))
                .toList();
        return cartRepository.save(new ShoppingCart(userId, items)).getItems();
    }

    private ShoppingCart.CartItem toCartItem(String productId, int quantity) {
        ProductInfo product = productClient.getProduct(productId);
        String imageUrl = product.imageUrls() != null && product.imageUrls().length > 0
                ? product.imageUrls()[0]
                : "";
        return new ShoppingCart.CartItem(
                product.id(),
                product.name(),
                product.price(),
                quantity,
                imageUrl,
                product.userId(),
                product.quantity() == null ? 0 : product.quantity());
    }
}
