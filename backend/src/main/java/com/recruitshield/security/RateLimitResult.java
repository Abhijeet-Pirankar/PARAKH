package com.recruitshield.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitResult {

    private final boolean allowed;
    private final long retryAfterSeconds;
    private final String reason;

    public static RateLimitResult allowed() {
        return new RateLimitResult(true, 0, null);
    }

    public static RateLimitResult exceeded(long retryAfterSeconds, String reason) {
        return new RateLimitResult(false, retryAfterSeconds, reason);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String reason() {
        return reason;
    }
}
