package com.recruitshield.service;

import com.recruitshield.dto.AuthResponse;
import com.recruitshield.dto.LoginRequest;
import com.recruitshield.dto.RegisterRequest;
import com.recruitshield.dto.UserDto;
import com.recruitshield.entity.User;
import com.recruitshield.exception.DuplicateEmailException;
import com.recruitshield.exception.InvalidCredentialsException;
import com.recruitshield.repository.UserRepository;
import com.recruitshield.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException("Email is already registered: " + normalizedEmail);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword().trim());

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .passwordHash(encodedPassword)
                .role("USER")
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with ID {} and email {}", savedUser.getId(), savedUser.getEmail());

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole(), savedUser.getId(), savedUser.getName());

        return AuthResponse.of(token, savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        validateLoginRequest(request);

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        log.info("User {} successfully logged in", user.getEmail());

        String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId(), user.getName());

        return AuthResponse.of(token, user);
    }

    public UserDto getCurrentUser(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException("Authentication required.");
        }
        User user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("User not found."));
        return UserDto.fromEntity(user);
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty or blank.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty or blank.");
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty or blank.");
        }
        if (request.getPassword().trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty or blank.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty or blank.");
        }
    }
}
