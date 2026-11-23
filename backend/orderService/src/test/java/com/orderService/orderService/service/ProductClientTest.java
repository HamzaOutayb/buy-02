package com.orderService.orderService.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.orderService.orderService.dto.ProductInfo;

class ProductClientTest {

    private ProductClient productClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://product:8082");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        productClient = new ProductClient("http://product:8082");

        // inject the mocked restClient
        Field field = ProductClient.class.getDeclaredField("restClient");
        field.setAccessible(true);
        field.set(productClient, builder.build());
    }

    @Test
    void getProduct_success() {
        mockServer.expect(requestTo("http://product:8082/api/v1/products/p1"))
                .andRespond(withSuccess("{\"id\":\"p1\",\"name\":\"Product 1\",\"price\":10.0,\"quantity\":5,\"sellerId\":\"s1\"}", MediaType.APPLICATION_JSON));

        ProductInfo info = productClient.getProduct("p1");

        assertNotNull(info);
        assertEquals("p1", info.id());
        assertEquals("Product 1", info.name());
        mockServer.verify();
    }

    @Test
    void getProduct_nullBody_throwsResponseStatusException() {
        mockServer.expect(requestTo("http://product:8082/api/v1/products/p2"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> productClient.getProduct("p2"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        mockServer.verify();
    }

    @Test
    void getProduct_serverError_throwsResponseStatusException() {
        mockServer.expect(requestTo("http://product:8082/api/v1/products/p3"))
                .andRespond(withBadRequest());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> productClient.getProduct("p3"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        mockServer.verify();
    }
}
