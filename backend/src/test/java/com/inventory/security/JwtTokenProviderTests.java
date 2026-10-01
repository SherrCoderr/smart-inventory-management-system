package com.inventory.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTests {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("Generate and validate a valid JWT token")
    void testGenerateAndValidateToken() {
        String token = jwtTokenProvider.generateTokenFromUser(1L, "admin_user", "ADMIN");

        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("admin_user");
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Reject an invalid or malformed JWT token")
    void testValidateMalformedToken() {
        String malformedToken = "invalid.token.payload";
        assertThat(jwtTokenProvider.validateToken(malformedToken)).isFalse();
    }
}
