package com.example.orderservice.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.orderservice.model.Order;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByBuyerIdAndArchivedFalseOrderByCreatedAtDesc(String buyerId);
    List<Order> findBySellerIdAndArchivedFalseOrderByCreatedAtDesc(String sellerId);
    List<Order> findByBuyerIdAndCheckoutKey(String buyerId, String checkoutKey);
}
