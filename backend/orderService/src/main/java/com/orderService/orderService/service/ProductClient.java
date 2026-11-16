package com.orderService.orderService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.orderService.orderService.dto.ProductInfo;

@Component
public class ProductClient {
    private final RestClient restClient;

    public ProductClient(@Value("${product.service.url:http://product:8082}") String productServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(productServiceUrl).build();
    }

    public ProductInfo getProduct(String productId) {
        try {
            ProductInfo info = restClient.get()
                    .uri("/api/v1/products/{productId}", productId)
                    .retrieve()
                    .body(ProductInfo.class);
            if (info == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found: " + productId);
            }
            return info;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found: " + productId);
        }
    }
}
