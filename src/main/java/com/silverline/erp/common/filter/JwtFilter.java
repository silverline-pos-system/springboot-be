package com.silverline.erp.common.filter;

import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.service.JwtService;
import com.silverline.erp.module.auth.service.MyUserDetailsService;
import com.silverline.erp.module.manager.repository.SecondaryRoleAssignmentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;
    private final SecondaryRoleAssignmentRepository secondaryRoleRepo;

    public JwtFilter(JwtService jwtService, MyUserDetailsService userDetailsService,
                     SecondaryRoleAssignmentRepository secondaryRoleRepo) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.secondaryRoleRepo = secondaryRoleRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip JWT validation for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getTokenFromHeader(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = null;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            log.debug("JWT token extraction failed for request {}: {}", request.getRequestURI(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails user = userDetailsService.loadUserByUsername(username);
                if (jwtService.validateToken(token, user)) {
                    // Combine the primary-role authorities with any ACTIVE secondary role, so a temporarily
                    // assigned secondary role actually grants backend access (not just a UI hint). Looked up
                    // per request so revocation and expiry take effect immediately.
                    List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
                    if (user instanceof UserProfile up && up.getUserId() != null) {
                        secondaryRoleRepo
                                .findFirstByUserIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(up.getUserId(), LocalDateTime.now())
                                .ifPresent(a -> authorities.add(new SimpleGrantedAuthority("ROLE_" + a.getSecondaryRole())));
                    }
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UsernameNotFoundException e) {
                log.debug("User '{}' from JWT not found in database", username);
            } catch (Exception e) {
                log.warn("Error processing JWT for user '{}': {}", username, e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromHeader(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        // Fallback to query parameter (needed for standard EventSource/SSE connections)
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.trim().isEmpty()) {
            return tokenParam.trim();
        }
        return null;
    }
}
