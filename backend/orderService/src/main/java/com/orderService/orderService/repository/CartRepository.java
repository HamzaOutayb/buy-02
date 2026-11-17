package com.orderService.orderService.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orderService.orderService.model.ShoppingCart;

public interface CartRepository extends MongoRepository<ShoppingCart, String> {
}
