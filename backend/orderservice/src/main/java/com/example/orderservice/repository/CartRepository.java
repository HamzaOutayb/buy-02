package com.example.orderservice.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.orderservice.model.Cart;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByBuyerId(String buyerId);
    void deleteByBuyerId(String buyerId);
}
