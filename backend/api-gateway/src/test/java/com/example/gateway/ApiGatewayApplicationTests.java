package com.example.gateway;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@SpringBootTest
class ApiGatewayApplicationTests {
	@Autowired
	private CorsConfigurationSource corsConfigurationSource;

	@Test
	void contextLoads() {
	}

	@Test
	void corsAllowsConfiguredFrontendOrigin() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products");
		CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

		assertNotNull(configuration);
		assertTrue(configuration.checkOrigin("http://localhost:4200") != null);
	}

	@Test
	void corsAllowsPreflightRequestsWithCredentials() {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/products");
		CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

		assertNotNull(configuration);
		assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
		assertTrue(configuration.getAllowedMethods().contains("PATCH"));
		assertTrue(configuration.getAllowCredentials());
	}

}
