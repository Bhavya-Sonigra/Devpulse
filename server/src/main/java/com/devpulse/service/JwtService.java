package com.devpulse.service;

import com.devpulse.config.AppConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppConfig appConfig;

    public String generateToken(UUID userId, UUID teamId, String username) {
        SecretKey key = Keys.hmacShaKeyFor(
                appConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(userId.toString())
                .claim("teamId", teamId.toString())
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L))
                .signWith(key)
                .compact();
    }

    public Claims validateAndExtract(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                appConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}