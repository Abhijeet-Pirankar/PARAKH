package com.recruitshield;

import com.recruitshield.dto.AuthResponse;
import com.recruitshield.dto.LoginRequest;
import com.recruitshield.dto.RegisterRequest;
import com.recruitshield.entity.User;
import com.recruitshield.exception.DuplicateEmailException;
import com.recruitshield.exception.InvalidCredentialsException;
import com.recruitshield.repository.UserRepository;
import com.recruitshield.security.JwtService;
import com.recruitshield.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Successful registration encodes password with BCrypt and returns AuthResponse")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice Candidate")
                .email("Alice@Example.com")
                .password("supersecret123")
                .build();

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(101L);
            return u;
        });
        when(jwtService.generateToken(eq("alice@example.com"), eq("USER"), eq(101L), eq("Alice Candidate")))
                .thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("Alice Candidate", response.getName());
        assertEquals("USER", response.getRole());

        // Verify password hashing
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotEquals("supersecret123", savedUser.getPasswordHash(), "Plaintext password must never be stored");
        assertTrue(passwordEncoder.matches("supersecret123", savedUser.getPasswordHash()), "Password hash must match BCrypt");
    }

    @Test
    @DisplayName("Registration rejects duplicate email with DuplicateEmailException")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmailIgnoreCase("bob@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Registration rejects weak password (less than 6 characters)")
    void testRegisterWeakPassword() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Charlie")
                .email("charlie@example.com")
                .password("123")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("at least 6 characters"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Registration rejects invalid email format")
    void testRegisterInvalidEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("David")
                .email("not-an-email")
                .password("securePassword1")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Invalid email format"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Successful login verifies BCrypt password and returns AuthResponse")
    void testLoginSuccess() {
        String hashedPassword = passwordEncoder.encode("myPassword123");
        User existingUser = User.builder()
                .id(202L)
                .name("Emma")
                .email("emma@example.com")
                .passwordHash(hashedPassword)
                .role("USER")
                .build();

        when(userRepository.findByEmailIgnoreCase("emma@example.com")).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(eq("emma@example.com"), eq("USER"), eq(202L), eq("Emma")))
                .thenReturn("valid.login.jwt");

        LoginRequest request = LoginRequest.builder()
                .email("emma@example.com")
                .password("myPassword123")
                .build();

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("valid.login.jwt", response.getToken());
        assertEquals("emma@example.com", response.getEmail());
        assertEquals("Emma", response.getName());
    }

    @Test
    @DisplayName("Login with incorrect password throws InvalidCredentialsException")
    void testLoginInvalidPassword() {
        String hashedPassword = passwordEncoder.encode("correctPassword");
        User existingUser = User.builder()
                .id(303L)
                .name("Frank")
                .email("frank@example.com")
                .passwordHash(hashedPassword)
                .role("USER")
                .build();

        when(userRepository.findByEmailIgnoreCase("frank@example.com")).thenReturn(Optional.of(existingUser));

        LoginRequest request = LoginRequest.builder()
                .email("frank@example.com")
                .password("wrongPassword")
                .build();

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login with unknown email throws InvalidCredentialsException")
    void testLoginUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
                .email("unknown@example.com")
                .password("anyPassword")
                .build();

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
