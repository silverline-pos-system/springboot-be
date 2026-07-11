package com.silverline.erp.module.auth.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.module.auth.dto.*;
import com.silverline.erp.module.auth.service.AuthService;
import com.silverline.erp.module.auth.service.PasswordResetRequestService;
import com.silverline.erp.module.auth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "User registration, login, and supervisor validation APIs")
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;
    private final PasswordResetRequestService passwordResetRequestService;

    @Operation(summary = "Register a new user", description = "Creates a new user profile in PENDING status, awaiting admin approval")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDTO>> registerUser(@Valid @RequestBody RegisterRequestDTO userDetails) {
        log.info("Processing registration for user: {}", userDetails.getUsername());
        RegisterResponseDTO response = registrationService.registerUser(userDetails);

        if (response.getUserId() == null && response.getMessage() != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(response.getMessage()));
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @Operation(summary = "Get all branches", description = "Fetches a list of all active branches for registration")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branches fetched successfully")
    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<List<Branch>>> getBranches() {
        List<Branch> branches = registrationService.getAllBranches();
        return ResponseEntity.ok(ApiResponse.success("Branches fetched successfully", branches));
    }

    @Operation(summary = "Log in a user", description = "Authenticates a user and returns a JWT token if login is successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or account not active")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LogInResponseDTO>> login(@Valid @RequestBody LogInRequestDTO logInRequestDTO) {
        log.info("Processing login for user: {}", logInRequestDTO.getUsername());
        LogInResponseDTO response = authService.logInUser(logInRequestDTO.getUsername(), logInRequestDTO.getPassword());

        if (response.getUserId() == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(response.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Verify supervisor credentials", description = "Validates supervisor credentials for override actions (e.g. returns)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supervisor verified successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or insufficient role")
    @PostMapping("/verify-supervisor")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifySupervisor(@Valid @RequestBody LogInRequestDTO credentials) {
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

    @Operation(summary = "Check username availability", description = "Checks if a username already exists in the system")
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsername(@RequestParam String username) {
        boolean exists = registrationService.isUsernameExists(username);
        return ResponseEntity.ok(ApiResponse.success("Username check completed", Map.of("exists", exists)));
    }

    @Operation(summary = "Submit password reset request", description = "Creates a request for password reset that must be approved by an administrator")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request submitted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid username or validation failure")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody PasswordResetRequestDTO request) {
        log.info("Processing password reset request for: {}", request.getUsername());
        try {
            passwordResetRequestService.forgotPassword(request.getUsername(), request.getNewPassword(), request.getReason());
            return ResponseEntity.ok(ApiResponse.success("Password reset request submitted successfully. A system administrator will review and approve your request shortly."));
        } catch (RuntimeException e) {
            log.warn("Password reset request failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
