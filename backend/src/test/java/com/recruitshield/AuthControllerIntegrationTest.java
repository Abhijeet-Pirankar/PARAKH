package com.recruitshield;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitshield.dto.LoginRequest;
import com.recruitshield.dto.RegisterRequest;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.repository.UserRepository;
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
}
