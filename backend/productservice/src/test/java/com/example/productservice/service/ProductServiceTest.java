package com.example.productservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.exception.ForbiddenException;
import com.example.productservice.exception.ProductNotFoundException;
import com.example.productservice.mapper.ProductMapper;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, ProductService.ProductDeletedEvent> kafkaTemplate;

    private ProductService productService;
    private ProductMapper productMapper;
    private Product product;
    private ProductRequest request;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapper();
        productService = new ProductService(productRepository, productMapper, kafkaTemplate);

        product = new Product();
        product.setId("product-1");
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(new BigDecimal("89.99"));
        product.setQuantity(4);
        product.setSellerId("seller-1");
        product.setImageUrls(List.of("https://res.cloudinary.com/demo/image/upload/v1/keyboard.jpg"));

        request = new ProductRequest();
        request.setName("Updated keyboard");
        request.setDescription("Updated description");
        request.setPrice(new BigDecimal("99.99"));
        request.setQuantity(8);
        request.setImageUrls(List.of("https://example.com/updated.jpg"));
    }

    @Test
    void getAllProducts_mapsProductsToResponses() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAllProducts();

        assertEquals(1, responses.size());
        assertEquals(product.getId(), responses.get(0).getId());
        assertEquals(product.getName(), responses.get(0).getName());
        assertEquals(product.getSellerId(), responses.get(0).getSellerId());
    }

    @Test
    void getAllProductsReturnsEmptyListWhenRepositoryIsEmpty() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<ProductResponse> responses = productService.getAllProducts();

        assertEquals(List.of(), responses);
    }

    @Test
    void getProductByIdMapsFoundProductToResponse() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(product.getId());

        assertEquals(product.getId(), response.getId());
        assertEquals(product.getName(), response.getName());
        assertEquals(product.getPrice(), response.getPrice());
        assertEquals(product.getSellerId(), response.getSellerId());
    }

    @Test
    void getProductById_throwsWhenProductDoesNotExist() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById("missing"));
    }

    @Test
    void createProduct_assignsSellerBeforeSaving() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(request, "seller-2");

        ArgumentCaptor<Product> savedProduct = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(savedProduct.capture());
        assertEquals("seller-2", savedProduct.getValue().getSellerId());
        assertEquals(request.getName(), response.getName());
    }

    @Test
    void updateProduct_updatesOnlyWhenSellerOwnsProduct() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = productService.updateProduct(product.getId(), request, "seller-1");

        assertEquals(request.getName(), product.getName());
        assertEquals(request.getPrice(), product.getPrice());
        assertEquals(request.getQuantity(), product.getQuantity());
        assertEquals("seller-1", response.getSellerId());
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_rejectsDifferentSeller() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(ForbiddenException.class,
                () -> productService.updateProduct(product.getId(), request, "seller-2"));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_throwsWhenProductDoesNotExist() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct("missing", request, "seller-1"));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_publishesDeletionEventAndDeletesProduct() throws Exception {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(kafkaTemplate.send(eq("images-deleted"), eq(product.getId()), any(ProductService.ProductDeletedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        productService.deleteProduct(product.getId(), "seller-1");

        ArgumentCaptor<ProductService.ProductDeletedEvent> event =
                ArgumentCaptor.forClass(ProductService.ProductDeletedEvent.class);
        verify(kafkaTemplate).send(eq("images-deleted"), eq(product.getId()), event.capture());
        assertEquals(product.getImageUrls(), event.getValue().imagePaths());
        verify(productRepository).deleteById(product.getId());
    }

    @Test
    void deleteProduct_throwsWhenProductDoesNotExist() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct("missing", "seller-1"));

        verify(kafkaTemplate, never()).send(
                any(String.class), any(String.class), any(ProductService.ProductDeletedEvent.class));
        verify(productRepository, never()).deleteById(any(String.class));
    }

    @Test
    void deleteProduct_rejectsDifferentSellerBeforePublishingEvent() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThrows(ForbiddenException.class,
                () -> productService.deleteProduct(product.getId(), "seller-2"));

        verify(kafkaTemplate, never()).send(
                any(String.class), any(String.class), any(ProductService.ProductDeletedEvent.class));
        verify(productRepository, never()).deleteById(any(String.class));
    }

    @Test
    void deleteProductDoesNotDeleteWhenEventPublishingFails() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(kafkaTemplate.send(eq("images-deleted"), eq(product.getId()),
                any(ProductService.ProductDeletedEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productService.deleteProduct(product.getId(), "seller-1"));

        assertEquals("Failed to publish product deletion event", exception.getMessage());
        assertEquals("broker unavailable", exception.getCause().getCause().getMessage());
        verify(productRepository, never()).deleteById(any(String.class));
    }

    @Test
    void deleteProductRestoresInterruptFlagWhenPublishingIsInterrupted() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(kafkaTemplate.send(eq("images-deleted"), eq(product.getId()),
                any(ProductService.ProductDeletedEvent.class)))
                .thenReturn(new CompletableFuture<>());

        Thread.currentThread().interrupt();
        try {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> productService.deleteProduct(product.getId(), "seller-1"));

            assertEquals("Interrupted while publishing product deletion event", exception.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(productRepository, never()).deleteById(any(String.class));
        } finally {
            Thread.interrupted();
        }
    }
}
