package com.product.productService.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.product.productService.config.ProductTopic;
import com.product.productService.dto.ProductEvent;
import com.product.productService.dto.ProductReq;
import com.product.productService.dto.ProductRes;
import com.product.productService.model.Product;
import com.product.productService.model.Categories;
import com.product.productService.reposiroty.ProductRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {
    private static final int MAX_IMAGE_URL_LENGTH = 500;

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;
    private final MediaClient mediaClient;

    public List<ProductRes> search(String keyword, List<String> categories) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<String> normalizedCategories = categories == null ? List.of() : categories;
        return productRepository.search(normalizedKeyword, normalizedCategories).stream()
                .map(product -> toResponse(product, mediaClient.getImageUrls(product.getId())))
                .toList();
    }

    public List<ProductRes> getAll() {
        return productRepository.findAll().stream().map(product -> toResponse(product, mediaClient.getImageUrls(product.getId())))
                .toList();
    }

    @PreAuthorize("hasRole('SELLER')")
    public List<ProductRes> getMine(Authentication authentication) {
        return productRepository.findAllByUserId(authentication.getName()).stream()
                .map(product -> toResponse(product, mediaClient.getImageUrls(product.getId())))
                .toList();
    }

    public ProductRes getById(String productId) {
        Product product = findProduct(productId);
        return toResponse(product, mediaClient.getImageUrls(product.getId()));
    }

    @PreAuthorize("hasRole('SELLER')")
    public ProductRes create(ProductReq req, Authentication authentication) {
        validateImageUrls(req.imageUrls());
        String[] categories = validateCategories(req.categories());

        Product product = new Product(null, req.name(), req.description(), req.price(), req.quantity(),
            authentication.getName(), categories);
        Product savedProduct = productRepository.save(product);
        ProductRes response = toResponse(savedProduct, req.imageUrls());
        sendProduct(new ProductEvent("CREATE", savedProduct.getId(), req.imageUrls()));
        return response;
    }

    public void sendProduct(ProductEvent productEvent) {
        kafkaTemplate.send(ProductTopic.NAME, productEvent.productId(), productEvent);
    }

    @PreAuthorize("hasRole('SELLER')")
    public ProductRes update(String productId, ProductReq req, Authentication authentication) {
        Product product = findOwnedProduct(productId, authentication);
        validateImageUrls(req.imageUrls());
        String[] categories = validateCategories(req.categories());

        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setQuantity(req.quantity());
        product.setCategories(categories);
        Product savedProduct = productRepository.save(product);
        sendProduct(new ProductEvent("UPDATE", savedProduct.getId(), req.imageUrls()));
        return toResponse(savedProduct, req.imageUrls());
    }

    @PreAuthorize("hasRole('SELLER')")
    public void delete(String productId, Authentication authentication) {
        Product product = findOwnedProduct(productId, authentication);
        productRepository.delete(product);
        sendProduct(new ProductEvent("DELETE", productId, null));
    }

    private Product findOwnedProduct(String productId, Authentication authentication) {
        Product product = findProduct(productId);
        if (!authentication.getName().equals(product.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this product");
        }
        return product;
    }

    private Product findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void validateImageUrls(String[] imageUrls) {
        if (imageUrls != null && java.util.Arrays.stream(imageUrls)
                .anyMatch(imageUrl -> imageUrl == null || imageUrl.length() > MAX_IMAGE_URL_LENGTH)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Each image URL must be 500 characters or fewer");
        }
    }

    private String[] validateCategories(String[] categories) {
        String[] requestedCategories = categories == null ? new String[0] : categories;
        if (Arrays.stream(requestedCategories).anyMatch(category -> !Categories.isValid(category))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Each category must be one of the supported product categories");
        }
        return Arrays.stream(requestedCategories)
                .map(Categories::normalize)
                .distinct()
                .toArray(String[]::new);
    }

    private ProductRes toResponse(Product product, String[] imageUrls) {
        return new ProductRes(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
            product.getQuantity(), product.getUserId(), imageUrls, product.getCategories());
    }
}