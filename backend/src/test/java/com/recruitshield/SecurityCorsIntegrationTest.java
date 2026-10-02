package com.recruitshield;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SecurityCorsIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("1. CORS Preflight: OPTIONS /api/analyze-offer allows localhost:5173 origin")
    void testCorsPreflightAllowedOriginLocalhost() throws Exception {
        mockMvc.perform(options("/api/analyze-offer")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));
    }

    @Test
    @DisplayName("2. CORS Preflight: OPTIONS /api/analyze-offer allows 127.0.0.1:5173 origin")
    void testCorsPreflightAllowedOriginLoopbackIp() throws Exception {
        mockMvc.perform(options("/api/analyze-offer")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("3. CORS Preflight: Rejects unauthorized origin with 403 Forbidden")
    void testCorsPreflightRejectsUnauthorizedOrigin() throws Exception {
        mockMvc.perform(options("/api/analyze-offer")
                        .header(HttpHeaders.ORIGIN, "http://malicious-attacker.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("4. CORS Preflight on Protected Route: OPTIONS /api/auth/me succeeds without JWT token")
    void testCorsPreflightOnProtectedRouteWithoutAuth() throws Exception {
        // Browser preflight OPTIONS requests never send Authorization headers
        mockMvc.perform(options("/api/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("5. Cross-Origin Request: POST /api/analyze-offer includes CORS headers for allowed origin")
    void testCrossOriginActualRequestAllowedOrigin() throws Exception {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Standard software engineer offer letter with no upfront fee.")
                .companyName("Acme Inc")
                .build();

        mockMvc.perform(post("/api/analyze-offer")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("6. Cross-Origin Request: POST from unauthorized origin does NOT receive CORS header")
    void testCrossOriginActualRequestUnauthorizedOrigin() throws Exception {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Offer from unauthorized origin.")
                .build();

        mockMvc.perform(post("/api/analyze-offer")
                        .header(HttpHeaders.ORIGIN, "http://untrusted-site.xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("7. Route Authorization: Unauthenticated request to protected /api/auth/me preserves CORS header for frontend")
    void testUnauthenticatedMeRequestWithCorsHeader() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("8. Route Authorization: Undefined route rejects unauthenticated request with 401")
    void testUndefinedRouteRejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/undefined-endpoint"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Full authentication is required")));
    }

    @Test
    @DisplayName("9. Route Authorization: Authenticated request to /api/auth/me from allowed origin receives 200 and CORS header")
    void testAuthenticatedMeRequestWithCorsHeader() throws Exception {
        String uniqueEmail = "corsuser_" + System.currentTimeMillis() + "@example.com";
        com.recruitshield.dto.RegisterRequest regReq = com.recruitshield.dto.RegisterRequest.builder()
                .name("Cors User")
                .email(uniqueEmail)
                .password("ValidPass123")
                .build();

        org.springframework.test.web.servlet.MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String token = jsonNode.get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(jsonPath("$.email", is(uniqueEmail.toLowerCase())));
    }

    @Test
    @DisplayName("10. CSRF Configuration: Stateless REST API allows POST requests without CSRF token")
    void testCsrfDisabledForStatelessApi() throws Exception {
        // In a stateless JWT architecture, CSRF tokens should be disabled
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Offer without CSRF token.")
                .build();

        mockMvc.perform(post("/api/analyze-offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
