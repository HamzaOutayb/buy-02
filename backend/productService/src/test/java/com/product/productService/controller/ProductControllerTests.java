package com.product.productService.controller;

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

import com.product.productService.dto.ProductReq;
import com.product.productService.dto.ProductRes;
import com.product.productService.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTests {
    @Mock
    private ProductService productService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductController productController;

    @Test
    void getProductsDelegatesToService() {
        List<ProductRes> products = List.of(response("product-1"));
        when(productService.getAll()).thenReturn(products);

        assertEquals(products, productController.getProducts());
        verify(productService).getAll();
    }

    @Test
    void searchProductsDelegatesKeywordAndCategories() {
        List<String> categories = List.of("furniture", "home");
        List<ProductRes> products = List.of(response("product-1"));
        when(productService.search("chair", categories)).thenReturn(products);

        assertEquals(products, productController.searchProducts("chair", categories));
        verify(productService).search("chair", categories);
    }

    @Test
    void getMyProductsDelegatesAuthentication() {
        List<ProductRes> products = List.of(response("product-1"));
        when(productService.getMine(authentication)).thenReturn(products);

        assertEquals(products, productController.getMyProducts(authentication));
        verify(productService).getMine(authentication);
    }

    @Test
    void getProductDelegatesId() {
        ProductRes product = response("product-1");
        when(productService.getById("product-1")).thenReturn(product);

        assertEquals(product, productController.getProduct("product-1"));
        verify(productService).getById("product-1");
    }

    @Test
    void createProductDelegatesRequestAndAuthentication() {
        ProductReq request = request();
        ProductRes product = response("product-1");
        when(productService.create(request, authentication)).thenReturn(product);

        assertEquals(product, productController.createProduct(request, authentication));
        verify(productService).create(request, authentication);
    }

    @Test
    void updateProductDelegatesIdRequestAndAuthentication() {
        ProductReq request = request();
        ProductRes product = response("product-1");
        when(productService.update("product-1", request, authentication)).thenReturn(product);

        assertEquals(product, productController.updateProduct("product-1", request, authentication));
        verify(productService).update("product-1", request, authentication);
    }

    @Test
    void deleteProductDelegatesIdAndAuthentication() {
        productController.deleteProduct("product-1", authentication);

        verify(productService).delete("product-1", authentication);
    }

    private static ProductReq request() {
        return new ProductReq("name", "description", 10.0, 2, new String[] { "image-1" });
    }

    private static ProductRes response(String id) {
        return new ProductRes(id, "name", "description", 10.0, 2, "seller-1", new String[] { "image-1" });
    }
}