package com.orderService.orderService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.mongodb.uri=mongodb://localhost:27017/orderDB")
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
