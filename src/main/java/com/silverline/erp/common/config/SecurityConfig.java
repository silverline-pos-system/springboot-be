package com.silverline.erp.common.config;

import com.silverline.erp.common.filter.JwtFilter;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.module.auth.service.MyUserDetailsService;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@NullMarked
public class SecurityConfig {

    private final MyUserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;

    @Value("${rocs.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:3000}")
    private String allowedOrigins;

    @Autowired
    public SecurityConfig(MyUserDetailsService userDetailsService, JwtFilter jwtFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Centralized CORS configuration — single source of truth.
     * In production, set rocs.cors.allowed-origins env var to restrict origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // Allow Spring Security to pass-through async/error dispatches (essential for SSE / error handling)
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC, jakarta.servlet.DispatcherType.ERROR).permitAll()
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/auth/branches",
                                "/api/v1/system/**",
                                "/public/**",
                                "/api/docs",
                                "/api/docs/**",
                                "/api/v3/api-docs",
                                "/api/v3/api-docs/**",
                                "/api/swagger-ui/**",
                                "/api/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        // Health check endpoint
                        .requestMatchers("/api/v1/health/**").permitAll()

                        // Notification endpoints - any authenticated user
                        .requestMatchers("/api/v1/notifications/**").authenticated()

                        // POS endpoints - accessible by CASHIER, SUPERVISOR, MANAGER, SUPER_ADMIN
                        .requestMatchers("/api/v1/pos/**")
                        .hasAnyRole(Role.CASHIER.name(), Role.SUPERVISOR.name(), Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Inventory PO endpoints - accessible by CASHIER for supplier payments
                        .requestMatchers("/api/inventory/po/status/**", "/api/inventory/po/*/items", "/api/inventory/po/*/process")
                        .hasAnyRole(Role.CASHIER.name(), Role.SUPERVISOR.name(), Role.STORE_KEEPER.name(), Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Inventory endpoints - accessible by STORE_KEEPER, MANAGER, SUPER_ADMIN
                        .requestMatchers("/api/inventory/**")
                        .hasAnyRole(Role.STORE_KEEPER.name(), Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Dashboard endpoints - accessible by MANAGER, SUPER_ADMIN
                        .requestMatchers("/api/v1/dashboard/**")
                        .hasAnyRole(Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Password reset requests - accessible by MANAGER, and SUPER_ADMIN
                        .requestMatchers("/api/v1/admin/password-requests/**")
                        .hasAnyRole(Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // User management endpoints - accessible by MANAGER, and SUPER_ADMIN
                        .requestMatchers("/api/v1/admin/users/**")
                        .hasAnyRole(Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Dispatch payment requests
                        .requestMatchers("/api/v1/dispatch-payments/**")
                        .hasAnyRole(Role.CASHIER.name(), Role.SUPERVISOR.name(), Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Services / Repair jobs
                        .requestMatchers("/api/v1/services/**")
                        .hasAnyRole(Role.CASHIER.name(), Role.SUPERVISOR.name(), Role.MANAGER.name(), Role.SUPER_ADMIN.name(), Role.DTV_TECHNICIAN.name(), Role.MOBILE_TECHNICIAN.name())

                        // Finance accounting endpoints
                        .requestMatchers("/api/v1/finance/accounting/**")
                        .hasAnyRole(Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Analytics reports
                        .requestMatchers("/api/v1/analytics/reports/**")
                        .hasAnyRole(Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Sales lookup endpoints
                        .requestMatchers("/api/v1/sales/**", "/api/sales/**")
                        .hasAnyRole(Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Admin only endpoints
                        .requestMatchers("/api/v1/admin/**", "/api/admin/**")
                        .hasAnyRole(Role.SUPER_ADMIN.name())

                        // Secondary role "me" endpoint - any authenticated user
                        .requestMatchers("/api/v1/manager/secondary-roles/me").authenticated()

                        // Allow any authenticated user to log their activities (e.g. cashiers)
                        .requestMatchers("/api/v1/manager/activity/log").authenticated()

                        // Grant Store Keeper and Supervisor access to activity logs
                        .requestMatchers("/api/v1/manager/activity/**")
                        .hasAnyRole(Role.STORE_KEEPER.name(), Role.SUPERVISOR.name(), Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // Manager endpoints
                        .requestMatchers("/api/v1/manager/**")
                        .hasAnyRole(Role.SUPERVISOR.name(), Role.MANAGER.name(), Role.SUPER_ADMIN.name())

                        // All other requests need authentication
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: " + authException.getMessage() + "\"}");
                        }));
        return http.build();
    }
}
