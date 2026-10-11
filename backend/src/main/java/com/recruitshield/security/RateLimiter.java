package com.recruitshield.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

@Slf4j
@Component
public class RateLimiter {

    public static final int DEFAULT_REQUESTS_PER_MINUTE = 30;
    public static final int DEFAULT_REQUESTS_PER_HOUR = 120;
    public static final int DEFAULT_LOGIN_REQUESTS_PER_MINUTE = 10;
    public static final int DEFAULT_REGISTER_REQUESTS_PER_MINUTE = 5;
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final int CLEANUP_TRIGGER_THRESHOLD = 5000;

    @Getter
    private final boolean enabled;
    @Getter
    private final int requestsPerMinute;
    @Getter
    private final int requestsPerHour;
    @Getter
    private final int loginRequestsPerMinute;
    @Getter
    private final int registerRequestsPerMinute;
    private final LongSupplier timeSupplier;

    private static class ClientRecord {
        final Deque<Long> timestamps = new ArrayDeque<>();
    }

    private final ConcurrentHashMap<String, ClientRecord> clientRequestMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClientRecord> loginRequestMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClientRecord> registerRequestMap = new ConcurrentHashMap<>();

    @Autowired
    public RateLimiter(
            @Value("${rate.limit.enabled:true}") boolean enabled,
            @Value("${rate.limit.requests-per-minute:30}") int requestsPerMinute,
            @Value("${rate.limit.requests-per-hour:120}") int requestsPerHour,
            @Value("${rate.limit.auth.login-per-minute:10}") int loginRequestsPerMinute,
            @Value("${rate.limit.auth.register-per-minute:5}") int registerRequestsPerMinute) {
        this(enabled, requestsPerMinute, requestsPerHour, loginRequestsPerMinute, registerRequestsPerMinute, System::currentTimeMillis);
    }

    public RateLimiter(
            boolean enabled,
            int requestsPerMinute,
            int requestsPerHour) {
        this(enabled, requestsPerMinute, requestsPerHour, DEFAULT_LOGIN_REQUESTS_PER_MINUTE, DEFAULT_REGISTER_REQUESTS_PER_MINUTE, System::currentTimeMillis);
    }

    public RateLimiter(
            boolean enabled,
            int requestsPerMinute,
            int requestsPerHour,
            LongSupplier timeSupplier) {
        this(enabled, requestsPerMinute, requestsPerHour, DEFAULT_LOGIN_REQUESTS_PER_MINUTE, DEFAULT_REGISTER_REQUESTS_PER_MINUTE, timeSupplier);
    }

    public RateLimiter(
            boolean enabled,
            int requestsPerMinute,
            int requestsPerHour,
            int loginRequestsPerMinute,
            int registerRequestsPerMinute,
            LongSupplier timeSupplier) {
        this.enabled = enabled;
        this.requestsPerMinute = requestsPerMinute > 0 ? requestsPerMinute : DEFAULT_REQUESTS_PER_MINUTE;
        this.requestsPerHour = requestsPerHour > 0 ? requestsPerHour : DEFAULT_REQUESTS_PER_HOUR;
        this.loginRequestsPerMinute = loginRequestsPerMinute > 0 ? loginRequestsPerMinute : DEFAULT_LOGIN_REQUESTS_PER_MINUTE;
        this.registerRequestsPerMinute = registerRequestsPerMinute > 0 ? registerRequestsPerMinute : DEFAULT_REGISTER_REQUESTS_PER_MINUTE;
        this.timeSupplier = timeSupplier != null ? timeSupplier : System::currentTimeMillis;
    }

    public RateLimitResult checkRateLimit(String clientIp) {
        if (!enabled || clientIp == null || clientIp.isBlank()) {
            return RateLimitResult.allowed();
        }

        // Opportunistic eviction if map size exceeds threshold
        if (clientRequestMap.size() > CLEANUP_TRIGGER_THRESHOLD) {
            evictExpiredFromMap(clientRequestMap, ONE_HOUR_MS);
        }

        long now = timeSupplier.getAsLong();
        long oneMinuteAgo = now - ONE_MINUTE_MS;
        long oneHourAgo = now - ONE_HOUR_MS;

        ClientRecord record = clientRequestMap.computeIfAbsent(clientIp, k -> new ClientRecord());

        synchronized (record) {
            Deque<Long> timestamps = record.timestamps;

            // 1. Evict timestamps older than 1 hour window
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= oneHourAgo) {
                timestamps.pollFirst();
            }

            // 2. Count timestamps in the 1-minute window
            int minuteCount = 0;
            Long oldestInMinute = null;
            for (Long ts : timestamps) {
                if (ts > oneMinuteAgo) {
                    minuteCount++;
                    if (oldestInMinute == null) {
                        oldestInMinute = ts;
                    }
                }
            }

            // Check requests per minute limit
            if (minuteCount >= requestsPerMinute) {
                long retryAfterSec = oldestInMinute != null
                        ? Math.max(1, (oldestInMinute + ONE_MINUTE_MS - now + 999) / 1000)
                        : 60;
                log.warn("Rate limit (per-minute: {}/{}) exceeded for client IP {}",
                        minuteCount, requestsPerMinute, clientIp);
                return RateLimitResult.exceeded(retryAfterSec, "Requests per minute limit exceeded");
            }

            // Check requests per hour limit
            if (timestamps.size() >= requestsPerHour) {
                Long oldestInHour = timestamps.peekFirst();
                long retryAfterSec = oldestInHour != null
                        ? Math.max(1, (oldestInHour + ONE_HOUR_MS - now + 999) / 1000)
                        : 3600;
                log.warn("Rate limit (per-hour: {}/{}) exceeded for client IP {}",
                        timestamps.size(), requestsPerHour, clientIp);
                return RateLimitResult.exceeded(retryAfterSec, "Requests per hour limit exceeded");
            }

            // Both limits passed: record current request timestamp
            timestamps.addLast(now);
            return RateLimitResult.allowed();
        }
    }

    public RateLimitResult checkLoginRateLimit(String clientIp) {
        return checkMinuteWindowLimit(loginRequestMap, clientIp, loginRequestsPerMinute, "Login");
    }

    public RateLimitResult checkRegisterRateLimit(String clientIp) {
        return checkMinuteWindowLimit(registerRequestMap, clientIp, registerRequestsPerMinute, "Registration");
    }

    private RateLimitResult checkMinuteWindowLimit(
            ConcurrentHashMap<String, ClientRecord> map,
            String clientIp,
            int maxPerMinute,
            String actionName) {
        if (!enabled || clientIp == null || clientIp.isBlank()) {
            return RateLimitResult.allowed();
        }

        if (map.size() > CLEANUP_TRIGGER_THRESHOLD) {
            evictExpiredFromMap(map, ONE_MINUTE_MS);
        }

        long now = timeSupplier.getAsLong();
        long oneMinuteAgo = now - ONE_MINUTE_MS;

        ClientRecord record = map.computeIfAbsent(clientIp, k -> new ClientRecord());

        synchronized (record) {
            Deque<Long> timestamps = record.timestamps;

            while (!timestamps.isEmpty() && timestamps.peekFirst() <= oneMinuteAgo) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxPerMinute) {
                Long oldest = timestamps.peekFirst();
                long retryAfterSec = oldest != null
                        ? Math.max(1, (oldest + ONE_MINUTE_MS - now + 999) / 1000)
                        : 60;
                log.warn("Rate limit ({}: {}/{}) exceeded for client IP {}",
                        actionName, timestamps.size(), maxPerMinute, clientIp);
                return RateLimitResult.exceeded(retryAfterSec, actionName + " rate limit exceeded");
            }

            timestamps.addLast(now);
            return RateLimitResult.allowed();
        }
    }

    public int evictExpired() {
        int evictedAnalysis = evictExpiredFromMap(clientRequestMap, ONE_HOUR_MS);
        int evictedLogin = evictExpiredFromMap(loginRequestMap, ONE_MINUTE_MS);
        int evictedRegister = evictExpiredFromMap(registerRequestMap, ONE_MINUTE_MS);
        int totalEvicted = evictedAnalysis + evictedLogin + evictedRegister;
        if (totalEvicted > 0) {
            log.debug("Evicted {} expired client IP entries from rate limiter caches", totalEvicted);
        }
        return totalEvicted;
    }

    private int evictExpiredFromMap(ConcurrentHashMap<String, ClientRecord> map, long maxAgeMs) {
        long now = timeSupplier.getAsLong();
        long cutoff = now - maxAgeMs;
        int evictedCount = 0;

        Iterator<Map.Entry<String, ClientRecord>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ClientRecord> entry = iterator.next();
            ClientRecord record = entry.getValue();
            synchronized (record) {
                while (!record.timestamps.isEmpty() && record.timestamps.peekFirst() <= cutoff) {
                    record.timestamps.pollFirst();
                }
                if (record.timestamps.isEmpty()) {
                    iterator.remove();
                    evictedCount++;
                }
            }
        }
        return evictedCount;
    }

    public int getTrackedClientCount() {
        return clientRequestMap.size();
    }

    public int getTrackedLoginClientCount() {
        return loginRequestMap.size();
    }

    public int getTrackedRegisterClientCount() {
        return registerRequestMap.size();
    }

    public void reset() {
        clientRequestMap.clear();
        loginRequestMap.clear();
        registerRequestMap.clear();
    }
}
