package com.silverline.erp.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Request logging filter for monitoring and debugging.
 * Logs all incoming requests with timing information.
 */
@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip logging for health checks and static resources
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/health") || uri.startsWith("/ws/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (duration > 2000) {
                log.warn("SLOW REQUEST: {} {} -> {} ({}ms)", method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("ERROR REQUEST: {} {} -> {} ({}ms)", method, uri, status, duration);
            } else {
                log.debug("{} {} -> {} ({}ms)", method, uri, status, duration);
            }
        }
    }
}
