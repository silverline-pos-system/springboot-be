package com.silverline.erp.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting filter.
 * Limits requests per IP address per minute.
 * For production, consider using Redis-backed rate limiting.
 */
@Slf4j
@Component
@Order(0)
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rocs.rate-limit.requests-per-minute:60}")
    private int maxRequestsPerMinute;

    @Value("${rocs.rate-limit.login-attempts-per-minute:5}")
    private int maxLoginAttemptsPerMinute;

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();

        // Determine rate limit based on endpoint
        int limit = maxRequestsPerMinute;
        if (uri.contains("/auth/login") || uri.contains("/auth/forgot-password")) {
            limit = maxLoginAttemptsPerMinute;
            clientIp = clientIp + ":login"; // Separate bucket for login
        }

        // Skip rate limiting for health checks
        if (uri.startsWith("/api/v1/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitBucket bucket = buckets.computeIfAbsent(clientIp, k -> new RateLimitBucket());

        if (bucket.isRateLimited(limit)) {
            log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, uri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Simple sliding-window rate limit bucket.
     * Resets every minute.
     */
    private static class RateLimitBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean isRateLimited(int maxRequests) {
            long now = System.currentTimeMillis();
            // Reset window every 60 seconds
            if (now - windowStart > 60_000) {
                count.set(0);
                windowStart = now;
            }
            return count.incrementAndGet() > maxRequests;
        }
    }
}
