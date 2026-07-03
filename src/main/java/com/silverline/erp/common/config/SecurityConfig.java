package com.silverline.erp.common.config;

import com.silverline.erp.common.filter.JwtFilter;
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
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/system/**",
                                "/public/**",
                                "/api/docs/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        // Health check endpoint
                        .requestMatchers("/api/v1/health/**").permitAll()

                        // WebSocket endpoint
                        .requestMatchers("/ws/**").permitAll()

                        // Notification endpoints - any authenticated user
                        .requestMatchers("/api/v1/notifications/**").authenticated()

                        // POS endpoints - accessible by CASHIER, SUPERVISOR, MANAGER, ADMIN, SUPER_ADMIN
                        .requestMatchers("/api/v1/pos/**")
                        .hasAnyRole("CASHIER", "SUPERVISOR", "MANAGER", "ADMIN", "SUPER_ADMIN")

                        // Inventory PO endpoints - accessible by CASHIER for supplier payments
                        .requestMatchers("/api/inventory/po/status/**", "/api/inventory/po/*/items", "/api/inventory/po/*/process")
                        .hasAnyRole("CASHIER", "STORE_KEEPER", "MANAGER", "ADMIN", "SUPER_ADMIN")

                        // Inventory endpoints - accessible by STORE_KEEPER, MANAGER, ADMIN, SUPER_ADMIN
                        .requestMatchers("/api/inventory/**")
                        .hasAnyRole("STORE_KEEPER", "MANAGER", "ADMIN", "SUPER_ADMIN")

                        // Dashboard endpoints - accessible by MANAGER, ADMIN, SUPER_ADMIN
                        .requestMatchers("/api/v1/dashboard/**")
                        .hasAnyRole("MANAGER", "ADMIN", "SUPER_ADMIN")

                        // Password reset requests - accessible by MANAGER, ADMIN, and SUPER_ADMIN
                        .requestMatchers("/api/v1/admin/password-requests/**")
                        .hasAnyRole("MANAGER", "ADMIN", "SUPER_ADMIN")

                        // Admin only endpoints
                        .requestMatchers("/api/v1/admin/**", "/api/admin/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Secondary role "me" endpoint - any authenticated user
                        .requestMatchers("/api/v1/manager/secondary-roles/me").authenticated()

                        // Grant Store Keeper access to activity logs
                        .requestMatchers("/api/v1/manager/activity/**")
                        .hasAnyRole("STORE_KEEPER", "MANAGER", "ADMIN", "SUPER_ADMIN")

                        // Manager endpoints
                        .requestMatchers("/api/v1/manager/**")
                        .hasAnyRole("MANAGER", "ADMIN", "SUPER_ADMIN")

                        // All other requests need authentication
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()));
        return http.build();
    }
}
