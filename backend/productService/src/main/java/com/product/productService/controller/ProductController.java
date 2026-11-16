package com.product.productService.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.product.productService.dto.ProductReq;
import com.product.productService.dto.ProductRes;
import com.product.productService.service.ProductService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@AllArgsConstructor
@Validated
public class ProductController {
    private final ProductService productService;
    
    @GetMapping("/search")
    public List<ProductRes> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> categories) {
        return productService.search(keyword, categories);
    }

    @GetMapping
    public List<ProductRes> getProducts() {
        return productService.getAll();
    }

    @GetMapping("/mine")
    public List<ProductRes> getMyProducts(Authentication authentication) {
        return productService.getMine(authentication);
    }

    @GetMapping("/{id}")
    public ProductRes getProduct(@PathVariable String id) {
        return productService.getById(id);
    }

    @PostMapping
    public ProductRes createProduct(@Valid @RequestBody ProductReq req, Authentication authentication) {
        return productService.create(req, authentication);
    }

    @PutMapping("/{id}")
    public ProductRes updateProduct(@PathVariable String id, @Valid @RequestBody ProductReq req,
            Authentication authentication) {
        return productService.update(id, req, authentication);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable String id, Authentication authentication) {
        productService.delete(id, authentication);
    }
}