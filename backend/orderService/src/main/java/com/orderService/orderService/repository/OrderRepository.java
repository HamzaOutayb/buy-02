package com.orderService.orderService.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.orderService.orderService.enums.OrderStatus;
import com.orderService.orderService.model.Order;

public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByCustomerId(String customerId, Pageable pageable);
    Page<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status, Pageable pageable);
    Page<Order> findByCustomerIdAndCreatedAtBetween(String customerId, Instant from, Instant to, Pageable pageable);
    Page<Order> findByCustomerIdAndStatusAndCreatedAtBetween(String customerId, OrderStatus status, Instant from, Instant to, Pageable pageable);
    Optional<Order> findByOrderReferenceAndCustomerId(String ref, String customerId);
    boolean existsByOrderReferenceAndCustomerId(String ref, String customerId);
    Page<Order> findByItemsSellerId(String sellerId, Pageable pageable);
    Page<Order> findByItemsSellerIdAndStatus(String sellerId, OrderStatus status, Pageable pageable);
    Page<Order> findByItemsSellerIdAndCreatedAtBetween(String sellerId, Instant from, Instant to, Pageable pageable);
    Page<Order> findByItemsSellerIdAndStatusAndCreatedAtBetween(String sellerId, OrderStatus status, Instant from, Instant to, Pageable pageable);
    Optional<Order> findByIdAndItemsSellerId(String id, String sellerId);
}
