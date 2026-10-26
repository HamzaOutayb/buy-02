package com.mediaservice.mediaservice.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_CLIENT", "ROLE_SELLER");

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public Claims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);
        Date expiration = claims.getExpiration();

        if (subject == null || subject.isBlank() || userId == null || userId.isBlank()
                || !ALLOWED_ROLES.contains(role)
                || expiration == null || !expiration.after(new Date())) {
            throw new JwtException("JWT is missing required or valid claims");
        }

        return claims;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
