package com.example.productservice.service;

import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductPageResponse;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.exception.ForbiddenException;
import com.example.productservice.exception.InsufficientStockException;
import com.example.productservice.exception.InvalidProductSearchException;
import com.example.productservice.exception.ProductNotFoundException;
import com.example.productservice.mapper.ProductMapper;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final KafkaTemplate<String, ProductDeletedEvent> kafkaTemplate;
    private final MongoTemplate mongoTemplate;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper,
            KafkaTemplate<String, ProductDeletedEvent> kafkaTemplate,
            MongoTemplate mongoTemplate) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.mongoTemplate = mongoTemplate;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public ProductPageResponse searchProducts(
            String keyword,
            String category,
            int page,
            int size) {
        validateSearch(keyword, category, page, size);

        List<Criteria> filters = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = Pattern.quote(keyword.trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("name").regex(safeKeyword, "i"),
                    Criteria.where("description").regex(safeKeyword, "i")));
        }
        if (category != null && !category.isBlank()) {
            filters.add(Criteria.where("category").regex(
                    "^" + Pattern.quote(category.trim()) + "$", "i"));
        }
        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(Criteria[]::new)));
        }
        Query countQuery = Query.of(query);
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));

        List<ProductResponse> products = mongoTemplate.find(query, Product.class).stream()
                .map(productMapper::toResponse)
                .toList();
        long totalElements = mongoTemplate.count(countQuery, Product.class);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new ProductPageResponse(products, page, size, totalElements, totalPages);
    }

    private void validateSearch(
            String keyword,
            String category,
            int page,
            int size) {
        if (keyword != null && keyword.length() > 100) {
            throw new InvalidProductSearchException("Keyword must not exceed 100 characters");
        }
        if (category != null && category.length() > 50) {
            throw new InvalidProductSearchException("Category must not exceed 50 characters");
        }
        if (page < 0) {
            throw new InvalidProductSearchException("Page must be zero or greater");
        }
        if (size < 1 || size > 50) {
            throw new InvalidProductSearchException("Page size must be between 1 and 50");
        }
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    public ProductResponse reserveStock(String productId, int quantity, String reservationId) {
        String reservationPath = "stockReservations." + reservationId;
        Query query = Query.query(Criteria.where("_id").is(productId)
                .and("quantity").gte(quantity)
                .and(reservationPath).exists(false));
        Update update = new Update()
                .inc("quantity", -quantity)
                .set(reservationPath, quantity);
        Product reserved = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Product.class);

        if (reserved == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));
            Integer existingReservation = product.getStockReservations() == null
                    ? null
                    : product.getStockReservations().get(reservationId);
            if (existingReservation != null && existingReservation == quantity) {
                return productMapper.toResponse(product);
            }
            throw new InsufficientStockException("Insufficient stock or invalid reservation for product " + productId);
        }
        return productMapper.toResponse(reserved);
    }

    public void releaseStock(String productId, String reservationId) {
        String reservationPath = "stockReservations." + reservationId;
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));
        Integer quantity = product.getStockReservations() == null
                ? null
                : product.getStockReservations().get(reservationId);
        if (quantity == null) {
            return;
        }
        Query query = Query.query(Criteria.where("_id").is(productId)
                .and(reservationPath).is(quantity));
        Update update = new Update()
                .inc("quantity", quantity)
                .unset(reservationPath);
        mongoTemplate.updateFirst(query, update, Product.class);
    }

    public ProductResponse createProduct(ProductRequest request, String sellerId) {
        Product product = productMapper.toEntity(request);
        product.setSellerId(sellerId);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    public ProductResponse updateProduct(String id, ProductRequest request, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("You are not the owner of this product");
        }

        productMapper.updateEntityFromRequest(product, request);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    public void deleteProduct(String id, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException(
                    "You are not the owner of this product");
        }

        ProductDeletedEvent event = new ProductDeletedEvent(product.getImageUrls());

        try {
            kafkaTemplate.send(
                    "images-deleted",
                    product.getId(),
                    event).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing product deletion event", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to publish product deletion event", e);
        }

        productRepository.deleteById(id);
    }

    public record ProductDeletedEvent(
            List<String> imagePaths) {
    }
}
