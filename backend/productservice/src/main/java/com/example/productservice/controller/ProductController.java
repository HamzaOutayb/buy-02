package com.example.productservice.controller;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.productservice.dto.ProductPageResponse;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.StockReservationRequest;
import com.example.productservice.security.InternalServiceTokenValidator;
import com.example.productservice.service.ProductService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final InternalServiceTokenValidator internalTokenValidator;

    public ProductController(
            ProductService productService,
            InternalServiceTokenValidator internalTokenValidator) {
        this.productService = productService;
        this.internalTokenValidator = internalTokenValidator;
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<ProductResponse> reserveStock(
            @PathVariable String id,
            @Valid @RequestBody StockReservationRequest request,
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String internalToken) {
        if (!internalTokenValidator.isValid(internalToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(productService.reserveStock(id, request.quantity(), request.reservationId()));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseStock(
            @PathVariable String id,
            @Valid @RequestBody StockReservationRequest request,
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String internalToken) {
        if (!internalTokenValidator.isValid(internalToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        productService.releaseStock(id, request.reservationId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/search")
    public ResponseEntity<ProductPageResponse> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(
                productService.searchProducts(q, category, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        String sellerId = authentication.getName();
        ProductResponse response = productService.createProduct(request, sellerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        String sellerId = authentication.getName();
        ProductResponse response = productService.updateProduct(id, request, sellerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id,
            Authentication authentication) {
        String sellerId = authentication.getName();
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}
