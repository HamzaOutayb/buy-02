package com.userservice.userservice.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms}")
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("A user ID is required to issue an access token");
        }
        String role = userDetails.getAuthorities().stream().findFirst()
                .map(authority -> authority.getAuthority())
                .orElse("ROLE_CLIENT");

        var builder = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration));
        return builder.signWith(getSigningKey()).compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        String role = extractClaims(token).get("role", String.class);
        if (role == null) {
            return "ROLE_CLIENT";
        } else {
            return role;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email != null && email.equalsIgnoreCase(userDetails.getUsername())
                && !isTokenExpired(extractClaims(token).getExpiration());
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
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
