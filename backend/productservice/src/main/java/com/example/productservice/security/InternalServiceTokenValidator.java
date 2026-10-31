package com.example.productservice.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceTokenValidator {
    @Value("${security.internal-service-token}")
    private String expectedToken;

    public boolean isValid(String suppliedToken) {
        return suppliedToken != null && MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8));
    }
}
