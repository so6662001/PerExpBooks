package com.qiankubx.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expireMs;

    public JwtUtil(@Value("${qianku.jwt.secret}") String secret,
                   @Value("${qianku.jwt.expire}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMs = expireSeconds * 1000;
    }

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
}
