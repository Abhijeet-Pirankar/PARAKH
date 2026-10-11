package com.recruitshield;

import com.recruitshield.security.RateLimitResult;
import com.recruitshield.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private AtomicLong simulatedClock;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        simulatedClock = new AtomicLong(1_000_000_000L); // 1,000,000 seconds
        rateLimiter = new RateLimiter(true, 30, 120, simulatedClock::get);
    }

    @Test
    @DisplayName("Under limit: 30 requests within 1 minute are all allowed")
    void testRequestsUnderMinuteLimitAllowed() {
        String clientIp = "192.168.1.10";

        for (int i = 1; i <= 30; i++) {
            RateLimitResult result = rateLimiter.checkRateLimit(clientIp);
            assertTrue(result.isAllowed(), "Request #" + i + " should be allowed");
            assertEquals(0, result.retryAfterSeconds());
            assertNull(result.reason());
            // Advance clock slightly (100ms per request)
            simulatedClock.addAndGet(100L);
        }
    }

    @Test
    @DisplayName("Exceeding limit: 31st request within 1 minute is rejected with Retry-After header")
    void testExceedingMinuteLimitRejectionAndRetryAfter() {
        String clientIp = "192.168.1.20";

        for (int i = 1; i <= 30; i++) {
            RateLimitResult result = rateLimiter.checkRateLimit(clientIp);
            assertTrue(result.isAllowed());
        }

        // 31st request in the exact same millisecond
        RateLimitResult throttledResult = rateLimiter.checkRateLimit(clientIp);
        assertFalse(throttledResult.isAllowed(), "31st request should be rejected");
        assertTrue(throttledResult.retryAfterSeconds() > 0, "Retry-After must be greater than 0");
        assertTrue(throttledResult.retryAfterSeconds() <= 60, "Retry-After must be <= 60 seconds");
        assertTrue(throttledResult.reason().contains("minute"));
    }

    @Test
    @DisplayName("IP isolation: Exceeding limit on one IP does not affect separate client IPs")
    void testSeparateClientIpsHaveIndependentLimits() {
        String clientA = "10.0.0.1";
        String clientB = "10.0.0.2";

        // Exhaust limit for Client A
        for (int i = 1; i <= 30; i++) {
            assertTrue(rateLimiter.checkRateLimit(clientA).isAllowed());
        }
        assertFalse(rateLimiter.checkRateLimit(clientA).isAllowed(), "Client A should be throttled");

        // Client B should still be completely unthrottled
        for (int i = 1; i <= 30; i++) {
            RateLimitResult resultB = rateLimiter.checkRateLimit(clientB);
            assertTrue(resultB.isAllowed(), "Client B request #" + i + " should be allowed");
        }
        // Client B request 31 should now be throttled
        assertFalse(rateLimiter.checkRateLimit(clientB).isAllowed(), "Client B request 31 should be throttled");
    }

    @Test
    @DisplayName("Window sliding: Client is unthrottled after 1 minute has elapsed without sleeps")
    void testRequestsRecoverAfterMinuteWindowPasses() {
        String clientIp = "172.16.0.5";

        for (int i = 1; i <= 30; i++) {
            rateLimiter.checkRateLimit(clientIp);
        }
        assertFalse(rateLimiter.checkRateLimit(clientIp).isAllowed());

        // Fast-forward simulated clock by 61 seconds (61,000 ms)
        simulatedClock.addAndGet(61_000L);

        // Client should be allowed again
        RateLimitResult recoveredResult = rateLimiter.checkRateLimit(clientIp);
        assertTrue(recoveredResult.isAllowed(), "Client should be allowed after minute window expires");
    }

    @Test
    @DisplayName("Sustained hour limit: 121st request across 1 hour is rejected even if under minute limit")
    void testSustainedHourLimitExceeded() {
        String clientIp = "192.168.100.1";

        // Send 30 requests every 2 minutes across 8 minutes (total 120 requests)
        for (int batch = 0; batch < 4; batch++) {
            for (int r = 0; r < 30; r++) {
                RateLimitResult res = rateLimiter.checkRateLimit(clientIp);
                assertTrue(res.isAllowed(), "Batch " + batch + " request " + r + " should be allowed");
            }
            // Advance clock by 2 minutes (120,000 ms) between batches
            simulatedClock.addAndGet(120_000L);
        }

        // Total 120 requests sent in ~6 minutes. 121st request should hit the 120 requests/hour limit!
        RateLimitResult throttledResult = rateLimiter.checkRateLimit(clientIp);
        assertFalse(throttledResult.isAllowed(), "121st request should exceed the 120/hr sustained limit");
        assertTrue(throttledResult.retryAfterSeconds() > 0);
        assertTrue(throttledResult.reason().contains("hour"));

        // Fast-forward by 1 hour (3,600,001 ms)
        simulatedClock.addAndGet(3_600_001L);

        // Client should be allowed again after 1 hour window expires
        assertTrue(rateLimiter.checkRateLimit(clientIp).isAllowed(), "Client should be allowed after 1 hour expires");
    }

    @Test
    @DisplayName("Memory safety: Evicting expired entries purges client IPs inactive for > 1 hour")
    void testEvictExpiredRemovesOldEntries() {
        String client1 = "10.1.1.1";
        String client2 = "10.1.1.2";

        rateLimiter.checkRateLimit(client1);
        rateLimiter.checkRateLimit(client2);
        assertEquals(2, rateLimiter.getTrackedClientCount());

        // Advance clock by 30 minutes: neither entry has expired (> 1 hour)
        simulatedClock.addAndGet(1_800_000L);
        int evictedAt30Min = rateLimiter.evictExpired();
        assertEquals(0, evictedAt30Min);
        assertEquals(2, rateLimiter.getTrackedClientCount());

        // Client 2 makes another request at 30 minutes
        rateLimiter.checkRateLimit(client2);

        // Advance clock by another 35 minutes (total 65 minutes from client 1, 35 minutes from client 2)
        simulatedClock.addAndGet(2_100_000L);

        int evictedAt65Min = rateLimiter.evictExpired();
        assertEquals(1, evictedAt65Min, "Client 1 should be evicted since > 1 hour has elapsed");
        assertEquals(1, rateLimiter.getTrackedClientCount(), "Only Client 2 should remain");

        // Advance clock by another 30 minutes (total 65 minutes from client 2's last request)
        simulatedClock.addAndGet(1_800_000L);
        int evictedClient2 = rateLimiter.evictExpired();
        assertEquals(1, evictedClient2, "Client 2 should now be evicted");
        assertEquals(0, rateLimiter.getTrackedClientCount(), "Tracking map should be completely empty");
    }

    @Test
    @DisplayName("Configuration: Disabled rate limiter allows arbitrary request volume")
    void testDisabledLimiterAllowsAllRequests() {
        RateLimiter disabledLimiter = new RateLimiter(false, 30, 120, simulatedClock::get);
        String clientIp = "192.168.1.99";

        for (int i = 1; i <= 100; i++) {
            assertTrue(disabledLimiter.checkRateLimit(clientIp).isAllowed());
        }
    }

    @Test
    @DisplayName("Edge case: Null or blank client IP is handled safely without throwing exceptions")
    void testNullOrBlankClientIpAlwaysAllowed() {
        assertTrue(rateLimiter.checkRateLimit(null).isAllowed());
        assertTrue(rateLimiter.checkRateLimit("").isAllowed());
        assertTrue(rateLimiter.checkRateLimit("   ").isAllowed());
    }

    @Test
    @DisplayName("Configuration: Non-positive limits fall back to safe defaults")
    void testNonPositiveLimitsFallBackToDefaults() {
        RateLimiter fallbackLimiter = new RateLimiter(true, 0, -10, 0, -5, simulatedClock::get);
        assertEquals(RateLimiter.DEFAULT_REQUESTS_PER_MINUTE, fallbackLimiter.getRequestsPerMinute());
        assertEquals(RateLimiter.DEFAULT_REQUESTS_PER_HOUR, fallbackLimiter.getRequestsPerHour());
        assertEquals(RateLimiter.DEFAULT_LOGIN_REQUESTS_PER_MINUTE, fallbackLimiter.getLoginRequestsPerMinute());
        assertEquals(RateLimiter.DEFAULT_REGISTER_REQUESTS_PER_MINUTE, fallbackLimiter.getRegisterRequestsPerMinute());
    }

    @Test
    @DisplayName("Auth Rate Limiting: 10 login requests within 1 minute are allowed")
    void testLoginRequestsUnderLimitAllowed() {
        String clientIp = "192.168.1.50";

        for (int i = 1; i <= 10; i++) {
            RateLimitResult result = rateLimiter.checkLoginRateLimit(clientIp);
            assertTrue(result.isAllowed(), "Login request #" + i + " should be allowed");
            assertEquals(0, result.retryAfterSeconds());
            assertNull(result.reason());
            simulatedClock.addAndGet(100L);
        }
    }

    @Test
    @DisplayName("Auth Rate Limiting: 11th login request within 60 seconds returns 429 and Retry-After")
    void testLoginRequest11ExceedsLimitAndReturnsRetryAfter() {
        String clientIp = "192.168.1.51";

        for (int i = 1; i <= 10; i++) {
            assertTrue(rateLimiter.checkLoginRateLimit(clientIp).isAllowed());
        }

        RateLimitResult throttled = rateLimiter.checkLoginRateLimit(clientIp);
        assertFalse(throttled.isAllowed(), "11th login request must be throttled");
        assertTrue(throttled.retryAfterSeconds() > 0 && throttled.retryAfterSeconds() <= 60);
        assertTrue(throttled.reason().contains("Login"));
    }

    @Test
    @DisplayName("Auth Rate Limiting: 5 registration requests within 1 minute are allowed")
    void testRegisterRequestsUnderLimitAllowed() {
        String clientIp = "192.168.1.60";

        for (int i = 1; i <= 5; i++) {
            RateLimitResult result = rateLimiter.checkRegisterRateLimit(clientIp);
            assertTrue(result.isAllowed(), "Register request #" + i + " should be allowed");
            assertEquals(0, result.retryAfterSeconds());
            assertNull(result.reason());
            simulatedClock.addAndGet(100L);
        }
    }

    @Test
    @DisplayName("Auth Rate Limiting: 6th registration request within 60 seconds returns 429 and Retry-After")
    void testRegisterRequest6ExceedsLimitAndReturnsRetryAfter() {
        String clientIp = "192.168.1.61";

        for (int i = 1; i <= 5; i++) {
            assertTrue(rateLimiter.checkRegisterRateLimit(clientIp).isAllowed());
        }

        RateLimitResult throttled = rateLimiter.checkRegisterRateLimit(clientIp);
        assertFalse(throttled.isAllowed(), "6th registration request must be throttled");
        assertTrue(throttled.retryAfterSeconds() > 0 && throttled.retryAfterSeconds() <= 60);
        assertTrue(throttled.reason().contains("Registration"));
    }

    @Test
    @DisplayName("Auth Rate Limiting: Login and Registration quotas are independent")
    void testLoginAndRegisterQuotasAreIndependent() {
        String clientIp = "192.168.1.70";

        // Exhaust login quota (10 requests)
        for (int i = 1; i <= 10; i++) {
            assertTrue(rateLimiter.checkLoginRateLimit(clientIp).isAllowed());
        }
        assertFalse(rateLimiter.checkLoginRateLimit(clientIp).isAllowed(), "11th login must be throttled");

        // Registration quota must still be completely available (5 requests)
        for (int i = 1; i <= 5; i++) {
            assertTrue(rateLimiter.checkRegisterRateLimit(clientIp).isAllowed(), "Register #" + i + " should be allowed");
        }
        assertFalse(rateLimiter.checkRegisterRateLimit(clientIp).isAllowed(), "6th register must be throttled");

        // Offer analysis quota should also remain completely available (30 requests)
        for (int i = 1; i <= 30; i++) {
            assertTrue(rateLimiter.checkRateLimit(clientIp).isAllowed(), "Analysis #" + i + " should be allowed");
        }
        assertFalse(rateLimiter.checkRateLimit(clientIp).isAllowed(), "31st analysis must be throttled");
    }

    @Test
    @DisplayName("Auth Rate Limiting: Different client IPs have independent auth limits")
    void testAuthRateLimitIpIsolation() {
        String ipA = "10.0.0.10";
        String ipB = "10.0.0.20";

        // Exhaust IP A login
        for (int i = 1; i <= 10; i++) {
            assertTrue(rateLimiter.checkLoginRateLimit(ipA).isAllowed());
        }
        assertFalse(rateLimiter.checkLoginRateLimit(ipA).isAllowed());

        // IP B login must be unthrottled
        for (int i = 1; i <= 10; i++) {
            assertTrue(rateLimiter.checkLoginRateLimit(ipB).isAllowed());
        }
        assertFalse(rateLimiter.checkLoginRateLimit(ipB).isAllowed());

        // Exhaust IP A register
        for (int i = 1; i <= 5; i++) {
            assertTrue(rateLimiter.checkRegisterRateLimit(ipA).isAllowed());
        }
        assertFalse(rateLimiter.checkRegisterRateLimit(ipA).isAllowed());

        // IP B register must be unthrottled
        for (int i = 1; i <= 5; i++) {
            assertTrue(rateLimiter.checkRegisterRateLimit(ipB).isAllowed());
        }
        assertFalse(rateLimiter.checkRegisterRateLimit(ipB).isAllowed());
    }

    @Test
    @DisplayName("Auth Rate Limiting: Auth limits recover after 60-second window elapses without real sleeps")
    void testAuthLimitsRecoverAfterMinuteExpires() {
        String clientIp = "172.16.10.1";

        for (int i = 1; i <= 10; i++) {
            rateLimiter.checkLoginRateLimit(clientIp);
        }
        assertFalse(rateLimiter.checkLoginRateLimit(clientIp).isAllowed());

        for (int i = 1; i <= 5; i++) {
            rateLimiter.checkRegisterRateLimit(clientIp);
        }
        assertFalse(rateLimiter.checkRegisterRateLimit(clientIp).isAllowed());

        // Advance simulated clock by 61 seconds
        simulatedClock.addAndGet(61_000L);

        // Both quotas should recover
        assertTrue(rateLimiter.checkLoginRateLimit(clientIp).isAllowed());
        assertTrue(rateLimiter.checkRegisterRateLimit(clientIp).isAllowed());
    }

    @Test
    @DisplayName("Auth Rate Limiting: Memory eviction purges expired login and register entries")
    void testAuthLimitsEvictExpired() {
        String client1 = "10.2.1.1";
        String client2 = "10.2.1.2";

        rateLimiter.checkLoginRateLimit(client1);
        rateLimiter.checkRegisterRateLimit(client1);
        rateLimiter.checkLoginRateLimit(client2);

        assertEquals(2, rateLimiter.getTrackedLoginClientCount());
        assertEquals(1, rateLimiter.getTrackedRegisterClientCount());

        // Advance 30 seconds: not expired (< 60s)
        simulatedClock.addAndGet(30_000L);
        int evictedAt30s = rateLimiter.evictExpired();
        assertEquals(0, evictedAt30s);

        // Advance another 35 seconds (total 65s)
        simulatedClock.addAndGet(35_000L);
        int evictedAt65s = rateLimiter.evictExpired();
        assertTrue(evictedAt65s >= 3, "All 3 auth records should be evicted");
        assertEquals(0, rateLimiter.getTrackedLoginClientCount());
        assertEquals(0, rateLimiter.getTrackedRegisterClientCount());
    }
}
