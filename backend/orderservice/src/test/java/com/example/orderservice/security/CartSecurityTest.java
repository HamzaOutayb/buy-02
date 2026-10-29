package com.example.orderservice.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import com.example.orderservice.controller.CartController;
import com.example.orderservice.dto.CartItemRequest;
import com.example.orderservice.dto.CartResponse;
import com.example.orderservice.service.CartService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(CartSecurityTest.TestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "security.jwt.secret=test-secret-key-for-cart-security-tests")
class CartSecurityTest {
    private static final String JWT_SECRET = "test-secret-key-for-cart-security-tests";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CartService cartService;

    @BeforeEach
    void setUp() {
        reset(cartService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void guestCannotAccessCart() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sellerCannotAccessCart() throws Exception {
        mockMvc.perform(get("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ROLE_SELLER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellerCannotAddItemsToCart() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ROLE_SELLER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"product-1\",\"quantity\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCanAccessTheirCart() throws Exception {
        when(cartService.getCart(anyString(), anyString()))
                .thenReturn(new CartResponse(List.of(), BigDecimal.ZERO));

        mockMvc.perform(get("/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ROLE_CLIENT")))
                .andExpect(status().isOk());

        verify(cartService).getCart(eq("user-1"), anyString());
    }

    @Test
    void clientCanAddItemsToCart() throws Exception {
        when(cartService.addItem(anyString(), any(CartItemRequest.class), anyString()))
                .thenReturn(new CartResponse(List.of(), BigDecimal.ZERO));

        mockMvc.perform(post("/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ROLE_CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"product-1\",\"quantity\":1}"))
                .andExpect(status().isOk());

        verify(cartService).addItem(eq("user-1"), any(CartItemRequest.class), anyString());
    }

    private String bearerToken(String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("client@example.com")
                .claim("userId", "user-1")
                .claim("role", role)
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
    static class TestConfig {
        @Bean
        CartService cartService() {
            return mock(CartService.class);
        }

        @Bean
        CartController cartController(CartService cartService) {
            return new CartController(cartService);
        }
    }
}
