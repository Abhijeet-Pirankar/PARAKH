package com.recruitshield;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitshield.dto.LoginRequest;
import com.recruitshield.dto.RegisterRequest;
import com.recruitshield.dto.VerifyRequest;
import com.recruitshield.security.RateLimiter;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class RateLimitIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        rateLimiter.reset();
    }

    @Test
    @DisplayName("Public Access: Unauthenticated request to /api/analyze-offer under limit succeeds without auth headers")
    void testPublicAccessUnderLimitSucceeds() throws Exception {
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Standard genuine software engineer offer with official interviews.")
                .companyName("Acme Corp")
                .companyWebsite("https://acme.org")
                .recruiterEmail("hr@acme.org")
                .receivedVia("LinkedIn")
                .build();

        mockMvc.perform(post("/api/analyze-offer")
                        .with(req -> {
                            req.setRemoteAddr("192.168.1.101");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", notNullValue()))
                .andExpect(jsonPath("$.riskScore", notNullValue()));
    }

    @Test
    @DisplayName("Rate Limiting: Exceeding 30 requests per minute on POST /api/analyze-offer returns 429 and Retry-After")
    void testExceedingLimitOnAnalyzeOfferReturns429() throws Exception {
        String testIp = "192.168.1.102";
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Genuine offer text testing rate limit.")
                .build();
        String jsonPayload = objectMapper.writeValueAsString(request);

        // Exhaust the 30 requests limit
        for (int i = 1; i <= 30; i++) {
            mockMvc.perform(post("/api/analyze-offer")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isOk());
        }

        // 31st request must be rejected with HTTP 429
        MvcResult throttledResult = mockMvc.perform(post("/api/analyze-offer")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error", is("TOO_MANY_REQUESTS")))
                .andExpect(jsonPath("$.message", containsString("Rate limit exceeded")))
                .andExpect(jsonPath("$.details", not(empty())))
                .andReturn();

        String retryAfterHeader = throttledResult.getResponse().getHeader("Retry-After");
        assertNotNull(retryAfterHeader);
        int retryAfter = Integer.parseInt(retryAfterHeader);
        assertTrue(retryAfter > 0 && retryAfter <= 60, "Retry-After should be between 1 and 60 seconds");
    }

    @Test
    @DisplayName("Rate Limiting: POST /api/verify legacy endpoint is also rate-limited when threshold is reached")
    void testVerifyEndpointIsRateLimited() throws Exception {
        String testIp = "192.168.1.103";
        String jsonPayload = """
            {
              "text": "Genuine interview process conducted on Microsoft Teams."
            }
            """;

        // Exhaust the 30 requests limit on /api/verify
        for (int i = 1; i <= 30; i++) {
            mockMvc.perform(post("/api/verify")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isOk());
        }

        // 31st request on /api/verify must be throttled
        mockMvc.perform(post("/api/verify")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error", is("TOO_MANY_REQUESTS")));
    }

    @Test
    @DisplayName("IP Isolation: Throttling client A does NOT affect client B")
    void testClientIpIsolationUnderLoad() throws Exception {
        String ipA = "192.168.1.104";
        String ipB = "192.168.1.105";

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Standard offer text for testing IP isolation.")
                .build();
        String jsonPayload = objectMapper.writeValueAsString(request);

        // Exhaust IP A
        for (int i = 1; i <= 30; i++) {
            mockMvc.perform(post("/api/analyze-offer")
                            .with(req -> {
                                req.setRemoteAddr(ipA);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/analyze-offer")
                        .with(req -> {
                            req.setRemoteAddr(ipA);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests());

        // IP B should be completely unaffected and receive 200 OK
        mockMvc.perform(post("/api/analyze-offer")
                        .with(req -> {
                            req.setRemoteAddr(ipB);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    @DisplayName("Selective Scope: Authentication and CORS preflight endpoints are NOT rate limited")
    void testUnrelatedEndpointsNotThrottled() throws Exception {
        String testIp = "192.168.1.106";

        VerifyRequest request = VerifyRequest.builder()
                .offerText("Offer text to exhaust analysis quota.")
                .build();
        String jsonPayload = objectMapper.writeValueAsString(request);

        // Exhaust analysis endpoint for this IP
        for (int i = 1; i <= 30; i++) {
            mockMvc.perform(post("/api/analyze-offer")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/analyze-offer")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests());

        // 1. OPTIONS preflight request to /api/analyze-offer must NOT be rate limited
        mockMvc.perform(options("/api/analyze-offer")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());

        // 2. POST /api/auth/login must NOT be rate limited by this limiter
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // Auth error, NOT 429
                .andExpect(jsonPath("$.error", is("INVALID_CREDENTIALS")));

        // 3. POST /api/auth/register must NOT be rate limited
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Rate Test")
                .email("ratetest_" + System.currentTimeMillis() + "@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated()); // 201 Created, NOT 429
    }

    @Test
    @DisplayName("Spoofing Protection: Client-supplied X-Forwarded-For is IGNORED from untrusted remote address")
    void testUntrustedForwardedForIsIgnored() throws Exception {
        String attackerActualIp = "198.51.100.50";
        VerifyRequest request = VerifyRequest.builder()
                .offerText("Offer text for spoofing test.")
                .build();
        String jsonPayload = objectMapper.writeValueAsString(request);

        // Attacker sends 30 requests spoofing X-Forwarded-For with unique fake IPs each time
        for (int i = 1; i <= 30; i++) {
            mockMvc.perform(post("/api/analyze-offer")
                            .with(req -> {
                                req.setRemoteAddr(attackerActualIp);
                                return req;
                            })
                            .header("X-Forwarded-For", "203.0.113." + i) // Fake client IPs
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isOk());
        }

        // Even with a completely new spoofed X-Forwarded-For, the 31st request is throttled
        // because the rate limiter keys on actual remoteAddr (198.51.100.50)
        mockMvc.perform(post("/api/analyze-offer")
                        .with(req -> {
                            req.setRemoteAddr(attackerActualIp);
                            return req;
                        })
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error", is("TOO_MANY_REQUESTS")));
    }

    @Test
    @DisplayName("Auth Rate Limiting: 10 login requests allowed; 11th returns 429 with Retry-After")
    void testLoginRateLimiting10Allowed11thReturns429() throws Exception {
        String testIp = "192.168.1.201";
        LoginRequest loginReq = LoginRequest.builder()
                .email("testuser@example.com")
                .password("WrongPassword123")
                .build();
        String jsonPayload = objectMapper.writeValueAsString(loginReq);

        // First 10 requests should reach authentication service (returns 401, not 429)
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error", is("INVALID_CREDENTIALS")));
        }

        // 11th request must be throttled with HTTP 429
        mockMvc.perform(post("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error", is("TOO_MANY_REQUESTS")))
                .andExpect(jsonPath("$.message", containsString("trying to log in again")));
    }

    @Test
    @DisplayName("Auth Rate Limiting: 5 registration requests allowed; 6th returns 429 with Retry-After")
    void testRegisterRateLimiting5Allowed6thReturns429() throws Exception {
        String testIp = "192.168.1.202";

        // First 5 registrations succeed (returns 201 Created)
        for (int i = 1; i <= 5; i++) {
            RegisterRequest regReq = RegisterRequest.builder()
                    .name("User " + i)
                    .email("ratereg_" + i + "_" + System.currentTimeMillis() + "@example.com")
                    .password("Password123")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token", notNullValue()));
        }

        // 6th registration request must be throttled with HTTP 429
        RegisterRequest excessReq = RegisterRequest.builder()
                .name("Excess User")
                .email("excess_" + System.currentTimeMillis() + "@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error", is("TOO_MANY_REQUESTS")))
                .andExpect(jsonPath("$.message", containsString("registering again")));
    }

    @Test
    @DisplayName("Auth Rate Limiting: Login and Registration quotas are completely independent")
    void testLoginAndRegisterQuotasIndependent() throws Exception {
        String testIp = "192.168.1.203";

        // Exhaust login quota (10 requests)
        LoginRequest loginReq = LoginRequest.builder()
                .email("independent@example.com")
                .password("WrongPassword")
                .build();
        String loginPayload = objectMapper.writeValueAsString(loginReq);

        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 11th login is throttled
        mockMvc.perform(post("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isTooManyRequests());

        // Registration from same IP is NOT throttled (quota is independent)
        RegisterRequest regReq = RegisterRequest.builder()
                .name("Independent User")
                .email("indep_" + System.currentTimeMillis() + "@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Auth Rate Limiting: Client IP isolation ensures IP A does not affect IP B")
    void testAuthRateLimitClientIpIsolation() throws Exception {
        String ipA = "192.168.1.205";
        String ipB = "192.168.1.206";

        LoginRequest loginReq = LoginRequest.builder()
                .email("isolation@example.com")
                .password("Password123")
                .build();
        String jsonPayload = objectMapper.writeValueAsString(loginReq);

        // Exhaust IP A
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(ipA);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(ipA);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests());

        // IP B is completely unaffected
        mockMvc.perform(post("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(ipB);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("Auth Rate Limiting: CORS OPTIONS requests to auth endpoints are never rate limited")
    void testCorsOptionsOnAuthEndpointsNotRateLimited() throws Exception {
        String testIp = "192.168.1.207";

        // Exhaust login quota for this IP
        LoginRequest loginReq = LoginRequest.builder()
                .email("corscheck@example.com")
                .password("Password123")
                .build();
        String jsonPayload = objectMapper.writeValueAsString(loginReq);

        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(testIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests());

        // OPTIONS preflight requests must succeed with 200 OK
        mockMvc.perform(options("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());

        mockMvc.perform(options("/api/auth/register")
                        .with(req -> {
                            req.setRemoteAddr(testIp);
                            return req;
                        })
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Auth Rate Limiting: Untrusted X-Forwarded-For cannot bypass login rate limit")
    void testUntrustedForwardedForCannotBypassAuthLimits() throws Exception {
        String attackerActualIp = "198.51.100.88";
        LoginRequest loginReq = LoginRequest.builder()
                .email("spoofattack@example.com")
                .password("Password123")
                .build();
        String jsonPayload = objectMapper.writeValueAsString(loginReq);

        // Attacker sends 10 login attempts spoofing X-Forwarded-For headers
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(attackerActualIp);
                                return req;
                            })
                            .header("X-Forwarded-For", "192.0.2." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 11th request with yet another spoofed IP is still throttled
        mockMvc.perform(post("/api/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(attackerActualIp);
                            return req;
                        })
                        .header("X-Forwarded-For", "192.0.2.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error", is("TOO_MANY_REQUESTS")));
    }
}
