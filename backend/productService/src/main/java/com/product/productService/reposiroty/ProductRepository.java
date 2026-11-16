package com.product.productService.reposiroty;

import java.util.List;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.product.productService.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
	List<Product> findAllByUserId(String userId);

	List<Product> findByNameContainingIgnoreCase(String keyword);

	@Aggregation(pipeline = {
			"{ '$match': { '$and': [ "
					+ "{ '$or': [ "
					+ "{ '$expr': { '$gte': [ { '$indexOfCP': [ { '$toLower': { '$ifNull': [ '$name', '' ] } }, ?0 ] }, 0 ] } }, "
					+ "{ '$expr': { '$gte': [ { '$indexOfCP': [ { '$toLower': { '$ifNull': [ '$description', '' ] } }, ?0 ] }, 0 ] } } "
					+ "] }, "
					+ "{ 'categories': { '$all': ?1 } } "
					+ "] } }"
	})
	List<Product> search(String keyword, List<String> categories);
}