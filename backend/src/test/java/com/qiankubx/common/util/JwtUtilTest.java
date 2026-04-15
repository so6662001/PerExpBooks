package com.qiankubx.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "test-secret-key-for-unit-testing-at-least-32-chars",
                86400
        );
    }

    @Test
    void generateToken_shouldContainUserId() {
        String token = jwtUtil.generateToken(12345L);

        assertThat(token).isNotBlank();
        Long userId = jwtUtil.getUserId(token);
        assertThat(userId).isEqualTo(12345L);
    }

    @Test
    void parseToken_validToken_shouldReturnUserId() {
        String token = jwtUtil.generateToken(99L);

        Claims claims = jwtUtil.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("99");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void parseToken_expiredToken_shouldThrow() {
        JwtUtil shortLivedJwt = new JwtUtil(
                "test-secret-key-for-unit-testing-at-least-32-chars",
                0
        );

        String token = shortLivedJwt.generateToken(1L);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThatThrownBy(() -> shortLivedJwt.parseToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseToken_invalidToken_shouldThrow() {
        assertThatThrownBy(() -> jwtUtil.parseToken("invalid.token.here"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseToken_tamperedToken_shouldThrow() {
        String token = jwtUtil.generateToken(1L);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtUtil.parseToken(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void generateToken_differentUsers_shouldReturnDifferentTokens() {
        String token1 = jwtUtil.generateToken(1L);
        String token2 = jwtUtil.generateToken(2L);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void getUserId_shouldReturnCorrectId() {
        String token = jwtUtil.generateToken(42L);

        Long userId = jwtUtil.getUserId(token);

        assertThat(userId).isEqualTo(42L);
    }
}
