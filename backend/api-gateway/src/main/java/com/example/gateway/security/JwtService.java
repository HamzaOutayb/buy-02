package com.example.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    @Value("${security.jwt.secret}")
    private String jwt_secret;

    public String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    public String extractRole(String token) {
        String role = extractClaims(token).get("role", String.class);
        if (role == null ){
            return "ROLE_CLIENT";
        }else{
            return role;
        }
    }

    public boolean isTokenValid(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject() != null && !isTokenExpired(claims.getExpiration());
    }

    private boolean isTokenExpired(Date expiration) {
        return expiration == null || expiration.before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwt_secret.getBytes(StandardCharsets.UTF_8));
    }
}