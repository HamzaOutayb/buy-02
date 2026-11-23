package com.orderService.orderService;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.mongodb.uri=mongodb://localhost:27017/orderDB")
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainDelegatesToSpringApplication() {
		String[] args = { "--spring.main.web-application-type=none" };
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			OrderServiceApplication.main(args);
			springApplication.verify(() -> SpringApplication.run(OrderServiceApplication.class, args));
		}
	}

}
