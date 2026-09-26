package com.recruitshield;

import com.recruitshield.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "test-mock-secret-key-unit-test-only-256-bits-length!";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Generate token and validate it successfully")
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken("user@example.com", "USER", 1L, "Test User");

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("user@example.com", jwtService.extractEmail(token));
        assertEquals("USER", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Invalid token should fail validation")
    void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";
        assertFalse(jwtService.validateToken(invalidToken));
        assertNull(jwtService.extractEmail(invalidToken));
    }

    @Test
    @DisplayName("Expired token should fail validation")
    void testExpiredToken() {
        // Create a service with negative expiration (already expired)
        JwtService expiredService = new JwtService(SECRET, -1000L);
        String token = expiredService.generateToken("expired@example.com", "USER", 2L, "Expired User");

        assertFalse(jwtService.validateToken(token));
        assertNull(jwtService.extractEmail(token));
    }

    @Test
    @DisplayName("Token generated with custom expiration works as expected")
    void testCustomExpirationToken() {
        String token = jwtService.generateToken(Map.of("role", "ADMIN"), "admin@example.com", 60000L);
        assertTrue(jwtService.validateToken(token));
        assertEquals("admin@example.com", jwtService.extractEmail(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
    }
}
