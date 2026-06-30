package com.nsbm.rocs.modules.auth.controller;

import com.nsbm.rocs.modules.auth.dto.*;
import com.nsbm.rocs.modules.auth.service.AuthService;
import com.nsbm.rocs.entity.main.Branch;
import com.nsbm.rocs.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@NullMarked
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDTO>> registerUser(@Valid @RequestBody RegisterRequestDTO userDetails) {
        log.info("Processing registration for user: {}", userDetails.getUsername());
        RegisterResponseDTO response = authService.registerUser(userDetails);

        if (response.getUserId() == null && response.getMessage() != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(response.getMessage()));
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<List<Branch>>> getBranches() {
        List<Branch> branches = authService.getAllBranches();
        return ResponseEntity.ok(ApiResponse.success("Branches fetched successfully", branches));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LogInResponseDTO>> login(@RequestBody LogInRequestDTO logInRequestDTO) {
        log.info("Processing login for user: {}", logInRequestDTO.getUsername());
        LogInResponseDTO response = authService.logInUser(logInRequestDTO.getUsername(), logInRequestDTO.getPassword());
        
        if (response.getUserId() == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(response.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/verify-supervisor")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifySupervisor(@RequestBody LogInRequestDTO credentials) {
        log.info("Processing supervisor verification for: {}", credentials.getUsername());
        boolean verified = authService.verifySupervisor(credentials.getUsername(), credentials.getPassword());
        
        if (verified) {
             return ResponseEntity.ok(ApiResponse.success("Supervisor verified successfully", 
                     Map.of("status", "verified")));
        } else {
             return ResponseEntity
                     .status(HttpStatus.UNAUTHORIZED)
                     .body(ApiResponse.error("Invalid supervisor credentials or insufficient permissions"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody PasswordResetRequestDTO request) {
        log.info("Processing password reset request for: {}", request.getUsername());
        try {
            authService.forgotPassword(request.getUsername(), request.getNewPassword(), request.getReason());
            return ResponseEntity.ok(ApiResponse.success("Password reset request submitted successfully. Please wait for admin approval."));
        } catch (RuntimeException e) {
            log.warn("Password reset request failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
