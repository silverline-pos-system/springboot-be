package com.silverline.erp.module.auth.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.function.Function;

public interface JwtService {
    String generateToken(Map<String, Object> claims, UserDetails userDetails);

    Claims extractAllClaims(String token);

    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    String extractUsername(String token);

    String getRoleClaim(String token);

    boolean isTokenExpired(String token);

    boolean validateToken(String token, UserDetails userDetails);
}
