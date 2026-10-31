package com.example.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
class OrderserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}
