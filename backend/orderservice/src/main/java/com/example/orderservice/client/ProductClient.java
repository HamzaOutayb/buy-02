package com.example.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.example.orderservice.dto.ProductSummary;

@Component
public class ProductClient {
    private final RestClient restClient;
    private final String internalServiceToken;

    public ProductClient(
            @Value("${services.product.base-url}") String productServiceUrl,
            @Value("${services.product.internal-token}") String internalServiceToken) {
        this.restClient = RestClient.builder().baseUrl(productServiceUrl).build();
        this.internalServiceToken = internalServiceToken;
    }

    public ProductSummary getProduct(String productId, String authorization) {
        return restClient.get()
                .uri("/products/{id}", productId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(ProductSummary.class);
    }

    public ProductSummary reserve(String productId, int quantity, String reservationId) {
        return restClient.post()
                .uri("/products/{id}/reserve", productId)
                .header("X-Internal-Service-Token", internalServiceToken)
                .body(new StockRequest(quantity, reservationId))
                .retrieve()
                .body(ProductSummary.class);
    }

    public void release(String productId, String reservationId) {
        restClient.post()
                .uri("/products/{id}/release", productId)
                .header("X-Internal-Service-Token", internalServiceToken)
                .body(new StockRequest(1, reservationId))
                .retrieve()
                .toBodilessEntity();
    }

    private record StockRequest(int quantity, String reservationId) {
    }
}
