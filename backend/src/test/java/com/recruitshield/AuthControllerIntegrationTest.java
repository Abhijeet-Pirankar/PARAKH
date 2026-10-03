package com.recruitshield;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitshield.dto.LoginRequest;
import com.recruitshield.dto.RegisterRequest;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.repository.UserRepository;
import com.recruitshield.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Public Registration: POST /api/auth/register creates user and returns JWT")
    void testRegisterEndpointSuccess() throws Exception {
        String uniqueEmail = "candidate_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest request = RegisterRequest.builder()
                .name("Alex Hunter")
                .email(uniqueEmail)
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.email", is(uniqueEmail.toLowerCase())))
                .andExpect(jsonPath("$.name", is("Alex Hunter")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Public Registration: POST /api/auth/register returns 409 Conflict on duplicate email")
    void testRegisterEndpointDuplicateEmail() throws Exception {
        String uniqueEmail = "duplicate_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest request = RegisterRequest.builder()
                .name("First User")
                .email(uniqueEmail)
                .password("Password123")
                .build();

        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_EMAIL")))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("Public Registration: POST /api/auth/register returns 400 Bad Request on weak password")
    void testRegisterEndpointWeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Short Pass")
                .email("shortpass_" + System.currentTimeMillis() + "@example.com")
                .password("123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("at least 6 characters")));
    }

    @Test
    @DisplayName("Public Login: POST /api/auth/login succeeds with valid credentials")
    void testLoginEndpointSuccess() throws Exception {
        String uniqueEmail = "loginuser_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Login Test User")
                .email(uniqueEmail)
                .password("SecretPassword456")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder()
                .email(uniqueEmail)
                .password("SecretPassword456")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.email", is(uniqueEmail.toLowerCase())))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Public Login: POST /api/auth/login returns 401 Unauthorized on invalid password")
    void testLoginEndpointInvalidPassword() throws Exception {
        String uniqueEmail = "wrongpass_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("User Wrong Pass")
                .email(uniqueEmail)
                .password("CorrectPassword123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder()
                .email(uniqueEmail)
                .password("IncorrectPassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Public Login: POST /api/auth/login returns 401 Unauthorized on unknown email")
    void testLoginEndpointUnknownEmail() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email("nonexistent_" + System.currentTimeMillis() + "@example.com")
                .password("AnyPassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("Protected Route: GET /api/auth/me rejects unauthenticated request with 401")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Protected Route: GET /api/auth/me rejects invalid token with 401")
    void testProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.token.payload"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Protected Route: GET /api/auth/me succeeds with valid JWT Bearer token")
    void testProtectedEndpointWithValidToken() throws Exception {
        String uniqueEmail = "protected_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Protected User")
                .email(uniqueEmail)
                .password("Pass456789")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = responseNode.get("token").asText();
        assertNotNull(token);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(uniqueEmail.toLowerCase())))
                .andExpect(jsonPath("$.name", is("Protected User")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("Public Compatibility: POST /api/analyze-offer still works WITHOUT any JWT token")
    void testAnalyzeOfferWorksWithoutJwt() throws Exception {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Offer without JWT! Pay Rs 1500 for training kit.")
                .companyName("NoAuth Test Ltd")
                .recruiterEmail("hr@noauth.com")
                .receivedVia("WhatsApp")
                .build();

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore", greaterThanOrEqualTo(40)))
                .andExpect(jsonPath("$.status", is("HIGHLY_SUSPICIOUS")))
                .andExpect(jsonPath("$.redFlags", not(empty())));
    }

    @Test
    @DisplayName("Public Compatibility: POST /api/verify legacy endpoint still works WITHOUT JWT token")
    void testLegacyVerifyWorksWithoutJwt() throws Exception {
        String jsonPayload = """
            {
              "text": "Genuine interview process conducted on Microsoft Teams with HR department."
            }
            """;

        mockMvc.perform(post("/api/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", notNullValue()))
                .andExpect(jsonPath("$.status", is("LIKELY_GENUINE")));
    }

    @Test
    @DisplayName("Security: Protected Route /api/auth/me rejects expired JWT with 401")
    void testProtectedEndpointWithExpiredToken() throws Exception {
        String expiredToken = jwtService.generateToken(Map.of("role", "USER"), "expired@example.com", -10000L);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security: Protected Route /api/auth/me rejects tampered JWT with 401")
    void testProtectedEndpointWithTamperedToken() throws Exception {
        String validToken = jwtService.generateToken("tampertest@example.com", "USER", 1L, "Tamper User");
        String tamperedToken = validToken.substring(0, validToken.lastIndexOf('.') + 1) + "corruptedSignature999";

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security: Protected Route /api/auth/me rejects empty Bearer header with 401")
    void testProtectedEndpointWithEmptyBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security: Protected Route /api/auth/me rejects Basic auth scheme with 401")
    void testProtectedEndpointWithBasicAuthScheme() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security: Protected Route /api/auth/me rejects malformed Bearer token with 401")
    void testProtectedEndpointWithMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not-even-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Registration: 5-character password boundary is rejected with 400 Bad Request")
    void testRegisterPasswordBoundary5CharsRejected() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Boundary Five")
                .email("boundary5_" + System.currentTimeMillis() + "@example.com")
                .password("12345")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("at least 6 characters")));
    }

    @Test
    @DisplayName("Registration: 6-character password boundary is accepted with 201 Created")
    void testRegisterPasswordBoundary6CharsAccepted() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Boundary Six")
                .email("boundary6_" + System.currentTimeMillis() + "@example.com")
                .password("123456")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("Registration: Missing or blank name is rejected with 400 Bad Request")
    void testRegisterMissingOrBlankName() throws Exception {
        RegisterRequest nullName = RegisterRequest.builder()
                .name(null)
                .email("nullname_" + System.currentTimeMillis() + "@example.com")
                .password("ValidPassword123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Name cannot be empty or blank")));

        RegisterRequest blankName = RegisterRequest.builder()
                .name("   ")
                .email("blankname_" + System.currentTimeMillis() + "@example.com")
                .password("ValidPassword123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Name cannot be empty or blank")));
    }

    @Test
    @DisplayName("Registration: Missing or blank email is rejected with 400 Bad Request")
    void testRegisterMissingOrBlankEmail() throws Exception {
        RegisterRequest nullEmail = RegisterRequest.builder()
                .name("Valid Name")
                .email(null)
                .password("ValidPassword123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Email cannot be empty or blank")));

        RegisterRequest blankEmail = RegisterRequest.builder()
                .name("Valid Name")
                .email("   ")
                .password("ValidPassword123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Email cannot be empty or blank")));
    }

    @Test
    @DisplayName("Registration: Missing or blank password is rejected with 400 Bad Request")
    void testRegisterMissingOrBlankPassword() throws Exception {
        RegisterRequest nullPassword = RegisterRequest.builder()
                .name("Valid Name")
                .email("nullpass_" + System.currentTimeMillis() + "@example.com")
                .password(null)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Password cannot be empty or blank")));

        RegisterRequest blankPassword = RegisterRequest.builder()
                .name("Valid Name")
                .email("blankpass_" + System.currentTimeMillis() + "@example.com")
                .password("     ")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Password cannot be empty or blank")));
    }

    @Test
    @DisplayName("Login Validation: Blank email or blank password is rejected with 400 Bad Request")
    void testLoginBlankEmailOrPassword() throws Exception {
        LoginRequest blankEmail = LoginRequest.builder()
                .email("   ")
                .password("ValidPassword123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Email cannot be empty or blank")));

        LoginRequest blankPassword = LoginRequest.builder()
                .email("valid@example.com")
                .password("   ")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Password cannot be empty or blank")));
    }

    @Test
    @DisplayName("Login: Mixed-case email registration allows case-insensitive login")
    void testLoginMixedCaseEmailResolution() throws Exception {
        String baseEmail = "MixedCaseUser_" + System.currentTimeMillis() + "@Example.COM";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Mixed Case Candidate")
                .email(baseEmail)
                .password("SecretPass123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login with lowercase
        LoginRequest lowerReq = LoginRequest.builder()
                .email(baseEmail.toLowerCase())
                .password("SecretPass123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.email", is(baseEmail.toLowerCase())));

        // Login with uppercase
        LoginRequest upperReq = LoginRequest.builder()
                .email(baseEmail.toUpperCase())
                .password("SecretPass123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upperReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.email", is(baseEmail.toLowerCase())));
    }

    @Test
    @DisplayName("Security: passwordHash is NEVER present in registration, login, or /api/auth/me responses")
    void testPasswordHashNeverExposedInResponses() throws Exception {
        String uniqueEmail = "privacy_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Privacy Test User")
                .email(uniqueEmail)
                .password("PrivacyPassword123")
                .build();

        // 1. Verify Register response
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        JsonNode regNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String token = regNode.get("token").asText();

        // 2. Verify Login response
        LoginRequest loginReq = LoginRequest.builder()
                .email(uniqueEmail)
                .password("PrivacyPassword123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());

        // 3. Verify /api/auth/me response
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Security: Registration rejects password exceeding 128 chars with 400 Bad Request (BCrypt CPU DoS protection)")
    void testRegisterPasswordExceeds128CharsRejected() throws Exception {
        String longPassword = "P".repeat(129);
        RegisterRequest request = RegisterRequest.builder()
                .name("Oversized Pass User")
                .email("longpass_" + System.currentTimeMillis() + "@example.com")
                .password(longPassword)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Password cannot exceed 128 characters")));
    }

    @Test
    @DisplayName("Security: Login rejects password exceeding 128 chars with 400 Bad Request (BCrypt CPU DoS protection)")
    void testLoginPasswordExceeds128CharsRejected() throws Exception {
        String longPassword = "P".repeat(129);
        LoginRequest request = LoginRequest.builder()
                .email("valid@example.com")
                .password(longPassword)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Password cannot exceed 128 characters")));
    }

    @Test
    @DisplayName("Security: Registration rejects name exceeding 100 chars with 400 Bad Request")
    void testRegisterNameExceeds100CharsRejected() throws Exception {
        String longName = "N".repeat(101);
        RegisterRequest request = RegisterRequest.builder()
                .name(longName)
                .email("longname_" + System.currentTimeMillis() + "@example.com")
                .password("ValidPass123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Name cannot exceed 100 characters")));
    }

    @Test
    @DisplayName("Security: Registration rejects email exceeding 255 chars with 400 Bad Request")
    void testRegisterEmailExceeds255CharsRejected() throws Exception {
        String longLocal = "e".repeat(250);
        String longEmail = longLocal + "@example.com";
        RegisterRequest request = RegisterRequest.builder()
                .name("Valid Name")
                .email(longEmail)
                .password("ValidPass123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Email cannot exceed 255 characters")));
    }
}
