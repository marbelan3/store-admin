package com.g2u.admin.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-signing",
                900000,
                604800000
        );
    }

    @Test
    void generateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = jwtTokenProvider.generateAccessToken(userId, tenantId, "test@example.com", "TENANT_ADMIN");

        assertTrue(jwtTokenProvider.validateToken(token));

        Claims claims = jwtTokenProvider.parseToken(token);
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("test@example.com", claims.get("email", String.class));
        assertEquals("TENANT_ADMIN", claims.get("role", String.class));
        assertEquals(tenantId.toString(), claims.get("tenantId", String.class));
    }

    @Test
    void generateAccessToken_withoutTenant() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.generateAccessToken(userId, null, "admin@example.com", "SUPER_ADMIN");

        Claims claims = jwtTokenProvider.parseToken(token);
        assertNull(claims.get("tenantId", String.class));
        assertEquals("SUPER_ADMIN", claims.get("role", String.class));
    }

    @Test
    void generateRefreshToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.generateRefreshToken(userId);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void invalidToken_shouldNotValidate() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void expiredToken_shouldNotValidate() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-signing",
                -1000,
                -1000
        );
        UUID userId = UUID.randomUUID();
        String token = shortLived.generateAccessToken(userId, null, "test@example.com", "TENANT_ADMIN");

        assertFalse(jwtTokenProvider.validateToken(token));
    }
}
