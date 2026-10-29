package com.example.orderservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.dto.CartItemRequest;
import com.example.orderservice.dto.CartItemResponse;
import com.example.orderservice.dto.CartResponse;
import com.example.orderservice.dto.ProductSummary;
import com.example.orderservice.exception.ApiException;
import com.example.orderservice.model.Cart;
import com.example.orderservice.repository.CartRepository;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public CartService(CartRepository cartRepository, ProductClient productClient) {
        this.cartRepository = cartRepository;
        this.productClient = productClient;
    }

    public CartResponse getCart(String buyerId, String authorization) {
        Cart cart = cartRepository.findByBuyerId(buyerId).orElseGet(() -> new CartForBuyer(buyerId).cart());
        return toResponse(cart, authorization);
    }

    public CartResponse addItem(String buyerId, CartItemRequest request, String authorization) {
        ProductSummary product = getProduct(request.productId(), authorization);
        if (product.quantity() == null || request.quantity() > product.quantity()) {
            throw new ApiException(HttpStatus.CONFLICT, "Requested quantity exceeds available stock");
        }

        Cart cart = cartRepository.findByBuyerId(buyerId).orElseGet(() -> new CartForBuyer(buyerId).cart());
        Cart.CartLine existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.productId()))
                .findFirst()
                .orElse(null);
        int nextQuantity = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        if (nextQuantity > product.quantity()) {
            throw new ApiException(HttpStatus.CONFLICT, "Requested quantity exceeds available stock");
        }
        if (existing == null) {
            cart.getItems().add(new Cart.CartLine(request.productId(), request.quantity()));
        } else {
            existing.setQuantity(nextQuantity);
        }
        cart.setUpdatedAt(Instant.now());
        return toResponse(cartRepository.save(cart), authorization);
    }

    public CartResponse updateItem(String buyerId, String productId, int quantity, String authorization) {
        ProductSummary product = getProduct(productId, authorization);
        if (product.quantity() == null || quantity > product.quantity()) {
            throw new ApiException(HttpStatus.CONFLICT, "Requested quantity exceeds available stock");
        }
        Cart cart = requireCart(buyerId);
        Cart.CartLine line = findLine(cart, productId);
        line.setQuantity(quantity);
        cart.setUpdatedAt(Instant.now());
        return toResponse(cartRepository.save(cart), authorization);
    }

    public CartResponse removeItem(String buyerId, String productId, String authorization) {
        Cart cart = requireCart(buyerId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cart.setUpdatedAt(Instant.now());
        if (cart.getItems().isEmpty()) {
            cartRepository.deleteByBuyerId(buyerId);
            return new CartResponse(List.of(), BigDecimal.ZERO);
        }
        return toResponse(cartRepository.save(cart), authorization);
    }

    private CartResponse toResponse(Cart cart, String authorization) {
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Cart.CartLine line : cart.getItems()) {
            ProductSummary product = getProduct(line.getProductId(), authorization);
            BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(line.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            items.add(new CartItemResponse(
                    product.id(), product.name(), product.category(), product.sellerId(),
                    product.imageUrls(), product.price(), line.getQuantity(), lineTotal));
        }
        return new CartResponse(items, subtotal);
    }

    private ProductSummary getProduct(String productId, String authorization) {
        try {
            ProductSummary product = productClient.getProduct(productId, authorization);
            if (product == null || product.id() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Product service returned an invalid response");
            }
            return product;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Product not found: " + productId);
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Product service could not provide product details");
        }
    }

    private Cart requireCart(String buyerId) {
        return cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cart not found"));
    }

    private Cart.CartLine findLine(Cart cart, String productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product is not in the cart"));
    }

    private record CartForBuyer(String buyerId) {
        Cart cart() {
            Cart cart = new Cart();
            cart.setBuyerId(buyerId);
            cart.setItems(new ArrayList<>());
            return cart;
        }
    }
}
