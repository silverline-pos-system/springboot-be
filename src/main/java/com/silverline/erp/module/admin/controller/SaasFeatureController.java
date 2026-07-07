package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.*;
import com.silverline.erp.module.admin.service.SaasFeatureService;
import com.silverline.erp.module.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/saas")
@RequiredArgsConstructor
@Tag(name = "SaaS Feature Flags Management", description = "APIs for dynamically enabling, disabling, and verifying ERP platform features and global system settings")
public class SaasFeatureController {

    private final SaasFeatureService saasFeatureService;
    private final UserRepository userRepository;

    // ============================
    // FEATURE MANAGEMENT
    // ============================

    @Operation(summary = "Get all SaaS features", description = "Retrieves a comprehensive list of all SaaS features and their status")
    @ApiResponse(responseCode = "200", description = "Features list fetched successfully")
    @GetMapping("/features")
    public ResponseEntity<List<SaasFeatureDTO>> getAllFeatures() {
        return ResponseEntity.ok(saasFeatureService.getAllFeatures());
    }

    @Operation(summary = "Get only active features", description = "Retrieves features that are currently active. Commonly queried by client UI modules to restrict views")
    @ApiResponse(responseCode = "200", description = "Active features list fetched successfully")
    @GetMapping("/features/active")
    public ResponseEntity<List<SaasFeatureDTO>> getActiveFeatures() {
        return ResponseEntity.ok(saasFeatureService.getActiveFeatures());
    }

    @Operation(summary = "Request feature toggle OTP", description = "Generates a 4-digit verification code and emails it to the configured administrator address to toggle a single feature")
    @ApiResponse(responseCode = "200", description = "OTP verification email dispatched successfully")
    @ApiResponse(responseCode = "400", description = "Feature state check or business validation error")
    @PostMapping("/features/request-toggle")
    public ResponseEntity<Map<String, String>> requestFeatureToggle(
            @RequestBody FeatureToggleRequest request) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(saasFeatureService.requestFeatureToggle(request, adminId));
    }

    @Operation(summary = "Request bulk feature toggle OTP", description = "Generates a single 4-digit verification code to activate/deactivate multiple premium features at once")
    @ApiResponse(responseCode = "200", description = "Bulk OTP email dispatched successfully")
    @ApiResponse(responseCode = "400", description = "Target features are already in requested state or not premium")
    @PostMapping("/features/request-toggle-all")
    public ResponseEntity<Map<String, String>> requestBulkFeatureToggle(
            @RequestBody BulkFeatureToggleRequest request) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(saasFeatureService.requestBulkFeatureToggle(request, adminId));
    }

    @Operation(summary = "Verify OTP and toggle single feature", description = "Validates the emailed OTP verification key and applies state toggle on the target feature")
    @ApiResponse(responseCode = "200", description = "Feature state toggled successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired OTP key code")
    @PostMapping("/features/verify-toggle")
    public ResponseEntity<SaasFeatureDTO> verifyAndToggleFeature(
            @RequestBody FeatureVerifyRequest request) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(saasFeatureService.verifyAndToggleFeature(request, adminId));
    }

    @Operation(summary = "Verify OTP and apply bulk toggle", description = "Validates the bulk OTP verification key and toggles states on multiple premium features")
    @ApiResponse(responseCode = "200", description = "Multiple features states updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification key")
    @PostMapping("/features/verify-toggle-all")
    public ResponseEntity<List<SaasFeatureDTO>> verifyAndToggleFeatures(
            @RequestBody BulkFeatureVerifyRequest request) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(saasFeatureService.verifyAndToggleFeatures(request, adminId));
    }

    // ============================
    // SYSTEM SETTINGS
    // ============================

    @Operation(summary = "Get system name", description = "Fetches the current brand/custom system name string")
    @ApiResponse(responseCode = "200", description = "System name retrieved successfully")
    @GetMapping("/settings/system-name")
    public ResponseEntity<Map<String, String>> getSystemName() {
        Map<String, String> response = Map.of("systemName", saasFeatureService.getSystemName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update system name", description = "Updates the global system name. Requires admin privileges.")
    @ApiResponse(responseCode = "200", description = "System name updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid empty system name input")
    @PutMapping("/settings/system-name")
    public ResponseEntity<Map<String, String>> updateSystemName(
            @RequestBody SystemSettingDTO dto) {
        Long adminId = getCurrentUserId();
        saasFeatureService.updateSystemName(dto.getSettingValue(), adminId);
        Map<String, String> response = Map.of(
                "message", "System name updated successfully",
                "systemName", saasFeatureService.getSystemName()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all system settings", description = "Fetches a map of all key-value configurations configured in system settings")
    @ApiResponse(responseCode = "200", description = "Settings map retrieved successfully")
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(saasFeatureService.getAllSettings());
    }

    // ============================
    // HELPER
    // ============================

    private Long getCurrentUserId() {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("User is not authenticated");
        }
        return userId;
    }
}
