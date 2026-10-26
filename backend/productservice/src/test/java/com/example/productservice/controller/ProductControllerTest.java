package com.example.productservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.exception.ForbiddenException;
import com.example.productservice.exception.GlobalExceptionHandler;
import com.example.productservice.exception.ProductNotFoundException;
import com.example.productservice.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private static final String SELLER_ID = "seller-1";

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void getAllProductsReturnsProductList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(productResponse()));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("product-1"))
                .andExpect(jsonPath("$[0].name").value("Keyboard"))
                .andExpect(jsonPath("$[0].sellerId").value(SELLER_ID));

        verify(productService).getAllProducts();
    }

    @Test
    void getProductByIdReturnsMatchingProduct() throws Exception {
        when(productService.getProductById("product-1")).thenReturn(productResponse());

        mockMvc.perform(get("/products/product-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("product-1"))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(89.99));

        verify(productService).getProductById("product-1");
    }

    @Test
    void createProductUsesAuthenticatedSellerAndReturnsCreatedProduct() throws Exception {
        when(productService.createProduct(any(ProductRequest.class), eq(SELLER_ID)))
                .thenReturn(productResponse());
        ArgumentCaptor<ProductRequest> requestCaptor = ArgumentCaptor.forClass(ProductRequest.class);

        mockMvc.perform(post("/products")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("product-1"))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.sellerId").value(SELLER_ID));

        verify(productService).createProduct(requestCaptor.capture(), eq(SELLER_ID));
        assertRequestValues(requestCaptor.getValue());
    }

    @Test
    void createProductRejectsInvalidRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/products")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "price": 0,
                                  "quantity": -1,
                                  "imageUrls": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));

        verify(productService, never()).createProduct(any(ProductRequest.class), any(String.class));
    }

    @Test
    void updateProductUsesAuthenticatedSellerAndReturnsUpdatedProduct() throws Exception {
        when(productService.updateProduct(eq("product-1"), any(ProductRequest.class), eq(SELLER_ID)))
                .thenReturn(productResponse());
        ArgumentCaptor<ProductRequest> requestCaptor = ArgumentCaptor.forClass(ProductRequest.class);

        mockMvc.perform(put("/products/product-1")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("product-1"))
                .andExpect(jsonPath("$.name").value("Keyboard"));

        verify(productService).updateProduct(eq("product-1"), requestCaptor.capture(), eq(SELLER_ID));
        assertRequestValues(requestCaptor.getValue());
    }

    @Test
    void updateProductRejectsInvalidRequestWithoutCallingService() throws Exception {
        mockMvc.perform(put("/products/product-1")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Keyboard",
                                  "price": null,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(productService, never()).updateProduct(
                any(String.class), any(ProductRequest.class), any(String.class));
    }

    @Test
    void getProductByIdReturnsNotFoundWhenServiceCannotFindProduct() throws Exception {
        when(productService.getProductById("missing"))
                .thenThrow(new ProductNotFoundException("Product not found with id: missing"));

        mockMvc.perform(get("/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: missing"));
    }

    @Test
    void updateProductReturnsForbiddenWhenSellerDoesNotOwnProduct() throws Exception {
        when(productService.updateProduct(eq("product-1"), any(ProductRequest.class), eq(SELLER_ID)))
                .thenThrow(new ForbiddenException("You are not the owner of this product"));

        mockMvc.perform(put("/products/product-1")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not the owner of this product"));
    }

    @Test
    void deleteProductReturnsNoContentForAuthenticatedSeller() throws Exception {
        mockMvc.perform(delete("/products/product-1").principal(authentication()))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct("product-1", SELLER_ID);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(SELLER_ID, "test-password");
    }

    private ProductResponse productResponse() {
        ProductResponse response = new ProductResponse();
        response.setId("product-1");
        response.setName("Keyboard");
        response.setDescription("Mechanical keyboard");
        response.setPrice(new BigDecimal("89.99"));
        response.setQuantity(4);
        response.setSellerId(SELLER_ID);
        response.setImageUrls(List.of("https://example.com/keyboard.jpg"));
        return response;
    }

    private String validProductJson() {
        return """
                {
                  "name": "Keyboard",
                  "description": "Mechanical keyboard",
                  "price": 89.99,
                  "quantity": 4,
                  "imageUrls": ["https://example.com/keyboard.jpg"]
                }
                """;
    }

    private void assertRequestValues(ProductRequest request) {
        assertEquals("Keyboard", request.getName());
        assertEquals(new BigDecimal("89.99"), request.getPrice());
        assertEquals(4, request.getQuantity());
        assertEquals(List.of("https://example.com/keyboard.jpg"), request.getImageUrls());
    }
}
