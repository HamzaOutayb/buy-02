package com.example.eureka_server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.netflix.discovery.EurekaClientConfig;

@SpringBootTest
class EurekaServerApplicationTests {
	@Autowired
	private EurekaClientConfig eurekaClientConfig;

	@Test
	void contextLoads() {
	}

	@Test
	void standaloneServerDoesNotRegisterWithEureka() {
		assertFalse(eurekaClientConfig.shouldRegisterWithEureka());
	}

	@Test
	void standaloneServerDoesNotFetchEurekaRegistry() {
		assertFalse(eurekaClientConfig.shouldFetchRegistry());
	}

}
