package com.recruitshield;

import com.recruitshield.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecretStartupIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtService.class);

    @Test
    @DisplayName("Startup fails when jwt.secret is not provided (missing)")
    void testStartupFailsWhenJwtSecretMissing() {
        contextRunner.run(context -> {
            assertNotNull(context.getStartupFailure(), "Context should have failed to start when secret is missing");
            Throwable rootCause = getRootCause(context.getStartupFailure());
            assertTrue(rootCause instanceof IllegalStateException, "Expected IllegalStateException as root cause");
            assertTrue(rootCause.getMessage().contains("JWT secret configuration is missing or blank"));
        });
    }

    @Test
    @DisplayName("Startup fails when jwt.secret is blank whitespace")
    void testStartupFailsWhenJwtSecretBlank() {
        contextRunner.withPropertyValues("jwt.secret=   ")
                .run(context -> {
                    assertNotNull(context.getStartupFailure(), "Context should have failed to start when secret is blank");
                    Throwable rootCause = getRootCause(context.getStartupFailure());
                    assertTrue(rootCause instanceof IllegalStateException);
                    assertTrue(rootCause.getMessage().contains("JWT secret configuration is missing or blank"));
                });
    }

    @Test
    @DisplayName("Startup fails when jwt.secret is empty string")
    void testStartupFailsWhenJwtSecretEmpty() {
        contextRunner.withPropertyValues("jwt.secret=")
                .run(context -> {
                    assertNotNull(context.getStartupFailure(), "Context should have failed to start when secret is empty");
                    Throwable rootCause = getRootCause(context.getStartupFailure());
                    assertTrue(rootCause instanceof IllegalStateException);
                    assertTrue(rootCause.getMessage().contains("JWT secret configuration is missing or blank"));
                });
    }

    @Test
    @DisplayName("Startup fails when jwt.secret is too weak (< 256 bits / 32 bytes)")
    void testStartupFailsWhenJwtSecretTooWeak() {
        contextRunner.withPropertyValues("jwt.secret=too-short-secret-key")
                .run(context -> {
                    assertNotNull(context.getStartupFailure(), "Context should have failed to start when secret is too weak");
                    Throwable rootCause = getRootCause(context.getStartupFailure());
                    assertTrue(rootCause instanceof IllegalStateException);
                    assertTrue(rootCause.getMessage().contains("JWT secret is too weak"));
                    assertFalse(rootCause.getMessage().contains("too-short-secret-key"), "Secret must never be leaked");
                });
    }

    @Test
    @DisplayName("Startup succeeds when jwt.secret is valid (>= 256 bits / 32 bytes)")
    void testStartupSucceedsWhenJwtSecretValid() {
        contextRunner.withPropertyValues("jwt.secret=valid-secure-key-dedicated-for-testing-only-32bytes")
                .run(context -> {
                    assertNull(context.getStartupFailure(), "Context should start successfully with valid secret");
                    JwtService jwtService = context.getBean(JwtService.class);
                    assertNotNull(jwtService);
                    String token = jwtService.generateToken("user@test.com", "USER", 1L, "User");
                    assertTrue(jwtService.validateToken(token));
                });
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
