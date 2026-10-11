package com.recruitshield.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitshield.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final Set<String> trustedProxies;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(
            RateLimiter rateLimiter,
            @Value("${rate.limit.trusted-proxies:}") String trustedProxiesConfig) {
        this.rateLimiter = rateLimiter;
        this.trustedProxies = parseTrustedProxies(trustedProxiesConfig);
    }

    private static Set<String> parseTrustedProxies(String config) {
        if (config == null || config.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        for (String proxy : config.split(",")) {
            String trimmed = proxy.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = extractNormalizedPath(request);
        return !(path.equals("/api/analyze-offer")
                || path.equals("/api/verify")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        String path = extractNormalizedPath(request);

        RateLimitResult result;
        String actionDescription;

        if ("/api/auth/login".equals(path)) {
            result = rateLimiter.checkLoginRateLimit(clientIp);
            actionDescription = "trying to log in again";
        } else if ("/api/auth/register".equals(path)) {
            result = rateLimiter.checkRegisterRateLimit(clientIp);
            actionDescription = "registering again";
        } else if ("/api/analyze-offer".equals(path) || "/api/verify".equals(path)) {
            result = rateLimiter.checkRateLimit(clientIp);
            actionDescription = "analyzing another offer";
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (!result.isAllowed()) {
            long retryAfter = result.getRetryAfterSeconds();
            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("TOO_MANY_REQUESTS")
                    .message("Rate limit exceeded. Please wait " + retryAfter + " second(s) before " + actionDescription + ".")
                    .details(List.of("Maximum allowed requests exceeded for client IP. Retry after " + retryAfter + " seconds."))
                    .timestamp(Instant.now().toString())
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractNormalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            remoteAddr = "unknown";
        }

        // Only inspect X-Forwarded-For if trusted reverse proxies are configured AND remoteAddr is trusted
        if (!trustedProxies.isEmpty() && (trustedProxies.contains(remoteAddr)
                || ("0:0:0:0:0:0:0:1".equals(remoteAddr) && trustedProxies.contains("127.0.0.1")))) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String[] ips = forwardedFor.split(",");
                for (String ip : ips) {
                    String trimmed = ip.trim();
                    if (!trimmed.isEmpty()) {
                        return trimmed;
                    }
                }
            }
        }

        return remoteAddr;
    }
}
