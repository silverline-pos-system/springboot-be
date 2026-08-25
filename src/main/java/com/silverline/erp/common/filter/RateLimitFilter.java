package com.silverline.erp.common.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiting filter, limiting requests per client per minute.
 * Buckets are held in a Caffeine cache that evicts idle entries automatically, so the map
 * cannot grow without bound (the previous ConcurrentHashMap never evicted -> memory-leak DoS).
 * For a multi-instance deployment, move to a shared store (e.g. Redis).
 */
@Slf4j
@Component
@Order(0)
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rocs.rate-limit.requests-per-minute:60}")
    private int maxRequestsPerMinute;

    @Value("${rocs.rate-limit.login-attempts-per-minute:5}")
    private int maxLoginAttemptsPerMinute;

    // Only honor X-Forwarded-For / X-Real-IP when explicitly behind a trusted reverse proxy.
    // Default false: otherwise any client can spoof the header to dodge the limit.
    @Value("${rocs.rate-limit.trust-forwarded-header:false}")
    private boolean trustForwardedHeader;

    // Auto-evicting bucket store: entries expire 2 minutes after last use, capped at 100k keys.
    private final Cache<String, RateLimitBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(100_000)
            .build();

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

        RateLimitBucket bucket = buckets.get(clientIp, k -> new RateLimitBucket());

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
        // By default use the real socket address, which a client cannot spoof. Proxy headers are
        // only trusted when explicitly enabled (deployment sits behind a known reverse proxy).
        if (trustForwardedHeader) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
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
