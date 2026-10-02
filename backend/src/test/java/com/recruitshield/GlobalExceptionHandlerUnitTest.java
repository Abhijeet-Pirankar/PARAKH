package com.recruitshield;

import com.recruitshield.dto.ErrorResponse;
import com.recruitshield.exception.DuplicateEmailException;
import com.recruitshield.exception.GlobalExceptionHandler;
import com.recruitshield.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 11 - Priority 10: GlobalExceptionHandler Unit Tests.
 *
 * Directly exercises each exception handler method in GlobalExceptionHandler in isolation:
 * 1. DuplicateEmailException -> 409 CONFLICT with DUPLICATE_EMAIL
 * 2. InvalidCredentialsException -> 401 UNAUTHORIZED with INVALID_CREDENTIALS
 * 3. BadCredentialsException -> 401 UNAUTHORIZED with INVALID_CREDENTIALS
 * 4. IllegalArgumentException -> 400 BAD_REQUEST with BAD_REQUEST
 * 5. HttpMessageNotReadableException -> 400 BAD_REQUEST with MALFORMED_REQUEST
 * 6. AccessDeniedException -> 403 FORBIDDEN with FORBIDDEN
 * 7. HttpRequestMethodNotSupportedException -> 405 METHOD_NOT_ALLOWED with METHOD_NOT_ALLOWED
 * 8. General Exception -> 500 INTERNAL_SERVER_ERROR with sanitized error message
 */
class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("1. DuplicateEmailException: Returns HTTP 409 Conflict with DUPLICATE_EMAIL error code")
    void testHandleDuplicateEmailException() {
        DuplicateEmailException ex = new DuplicateEmailException("Email is already registered: test@domain.com");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateEmailException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DUPLICATE_EMAIL", response.getBody().getError());
        assertEquals("Email is already registered: test@domain.com", response.getBody().getMessage());
        assertEquals(1, response.getBody().getDetails().size());
    }

    @Test
    @DisplayName("2. InvalidCredentialsException: Returns HTTP 401 Unauthorized with INVALID_CREDENTIALS")
    void testHandleInvalidCredentialsException() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid email or password.");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentialsException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getError());
        assertEquals("Invalid email or password.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("3. BadCredentialsException: Returns HTTP 401 Unauthorized with generic safe message")
    void testHandleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getError());
        assertEquals("Invalid email or password.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("4. IllegalArgumentException: Returns HTTP 400 Bad Request with BAD_REQUEST")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Offer text cannot be empty or blank.");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().getError());
        assertEquals("Offer text cannot be empty or blank.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("5. HttpMessageNotReadableException: Returns HTTP 400 Bad Request with MALFORMED_REQUEST")
    void testHandleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage("broken json".getBytes()));
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MALFORMED_REQUEST", response.getBody().getError());
        assertEquals("Malformed JSON request body.", response.getBody().getMessage());
        assertEquals("Malformed JSON body.", response.getBody().getDetails().get(0));
    }

    @Test
    @DisplayName("6. AccessDeniedException: Returns HTTP 403 Forbidden with FORBIDDEN")
    void testHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().getError());
        assertEquals("Access denied.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("7. HttpRequestMethodNotSupportedException: Returns HTTP 405 Method Not Allowed")
    void testHandleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");
        ResponseEntity<ErrorResponse> response = handler.handleHttpRequestMethodNotSupportedException(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("METHOD_NOT_ALLOWED", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("DELETE"));
    }

    @Test
    @DisplayName("8. General Exception: Returns HTTP 500 with sanitized message without leaking internals")
    void testHandleGeneralExceptionSanitized() {
        RuntimeException ex = new RuntimeException("Sensitive database stacktrace and internal connection details");
        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError());
        assertEquals("An unexpected error occurred.", response.getBody().getMessage());
        assertEquals(List.of("Please try again later."), response.getBody().getDetails());
        // Verify internal sensitive message is never exposed in response
        assertFalse(response.getBody().getMessage().contains("Sensitive database"));
        assertFalse(response.getBody().getDetails().get(0).contains("Sensitive database"));
    }
}
