package com.recruitshield;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitshield.entity.User;
import com.recruitshield.repository.UserRepository;
import com.recruitshield.security.CustomAuthenticationEntryPoint;
import com.recruitshield.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Stage 11 - Priority 10: Security Infrastructure Services Unit Tests.
 *
 * Verifies:
 * 1. CustomUserDetailsService:
 *    - Valid user loading and UserDetails generation
 *    - Case-insensitive email query handling
 *    - UsernameNotFoundException when user does not exist
 *    - Defaulting null or blank roles to "USER"
 *    - Stripping redundant "ROLE_" prefix in role mapping
 * 2. CustomAuthenticationEntryPoint:
 *    - Setting HTTP 401 Unauthorized status
 *    - Setting application/json Content-Type
 *    - Outputting structured ErrorResponse JSON
 *    - Null-safe handling of AuthenticationException
 */
@ExtendWith(MockitoExtension.class)
class SecurityInfrastructureTest {

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==========================================
    // CustomUserDetailsService Tests
    // ==========================================

    @Test
    @DisplayName("1. UserDetailsService: Successfully loads user and maps UserDetails credentials and authorities")
    void testLoadUserByUsernameSuccess() {
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        User user = User.builder()
                .id(1L)
                .name("Alice Developer")
                .email("alice@company.com")
                .passwordHash("$2a$10$hashedPasswordValue")
                .role("USER")
                .build();

        when(userRepository.findByEmailIgnoreCase("alice@company.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("alice@company.com");

        assertNotNull(userDetails);
        assertEquals("alice@company.com", userDetails.getUsername());
        assertEquals("$2a$10$hashedPasswordValue", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("2. UserDetailsService: Loads user case-insensitively with uppercase or mixed email")
    void testLoadUserByUsernameCaseInsensitive() {
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        User user = User.builder()
                .id(2L)
                .name("Bob Engineer")
                .email("bob@corp.org")
                .passwordHash("$2a$10$bobPasswordHash")
                .role("USER")
                .build();

        when(userRepository.findByEmailIgnoreCase("BOB@CORP.ORG")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("BOB@CORP.ORG");

        assertNotNull(userDetails);
        assertEquals("bob@corp.org", userDetails.getUsername());
    }

    @Test
    @DisplayName("3. UserDetailsService: Throws UsernameNotFoundException when email does not exist")
    void testLoadUserByUsernameNotFound() {
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown@domain.com"));

        assertTrue(ex.getMessage().contains("User not found with email: unknown@domain.com"));
    }

    @Test
    @DisplayName("4. UserDetailsService: Defaults null role to 'USER' without throwing exception")
    void testLoadUserByUsernameNullRoleDefaultsToUser() {
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        User user = User.builder()
                .id(3L)
                .name("Null Role User")
                .email("nullrole@domain.com")
                .passwordHash("$2a$10$somePasswordHash")
                .role(null)
                .build();

        when(userRepository.findByEmailIgnoreCase("nullrole@domain.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("nullrole@domain.com");

        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("5. UserDetailsService: Defaults blank or whitespace role to 'USER'")
    void testLoadUserByUsernameBlankRoleDefaultsToUser() {
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        User user = User.builder()
                .id(4L)
                .name("Blank Role User")
                .email("blankrole@domain.com")
                .passwordHash("$2a$10$somePasswordHash")
                .role("   ")
                .build();

        when(userRepository.findByEmailIgnoreCase("blankrole@domain.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("blankrole@domain.com");

        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("6. UserDetailsService: Strips redundant 'ROLE_' prefix cleanly when mapping roles")
    void testLoadUserByUsernameStripsRolePrefix() {
        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        User user = User.builder()
                .id(5L)
                .name("Admin User")
                .email("admin@domain.com")
                .passwordHash("$2a$10$somePasswordHash")
                .role("ROLE_ADMIN")
                .build();

        when(userRepository.findByEmailIgnoreCase("admin@domain.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("admin@domain.com");

        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    // ==========================================
    // CustomAuthenticationEntryPoint Tests
    // ==========================================

    @Test
    @DisplayName("7. AuthenticationEntryPoint: Sets HTTP 401 and Content-Type application/json")
    void testAuthenticationEntryPointCommenceHeaders() throws IOException {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Token required"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    @DisplayName("8. AuthenticationEntryPoint: Outputs structured ErrorResponse with UNAUTHORIZED code")
    void testAuthenticationEntryPointCommenceBody() throws IOException {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Invalid signature"));

        String body = response.getContentAsString();
        JsonNode root = objectMapper.readTree(body);

        assertEquals("UNAUTHORIZED", root.get("error").asText());
        assertEquals("Full authentication is required to access this resource.", root.get("message").asText());
        assertNotNull(root.get("details"));
        assertTrue(root.get("details").get(0).asText().contains("Invalid signature"));
    }

    @Test
    @DisplayName("9. AuthenticationEntryPoint: Null authException handled gracefully without throwing NPE")
    void testAuthenticationEntryPointNullAuthException() throws IOException {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertDoesNotThrow(() -> entryPoint.commence(request, response, null));

        assertEquals(401, response.getStatus());
        String body = response.getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        assertEquals("UNAUTHORIZED", root.get("error").asText());
        assertTrue(root.get("details").get(0).asText().contains("Unauthorized"));
    }
}
