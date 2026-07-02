package com.silverline.erp.module.admin.controller;

import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.dto.*;
import com.silverline.erp.module.admin.service.SaasFeatureService;
import com.silverline.erp.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/saas")
@RequiredArgsConstructor
public class SaasFeatureController {

    private final SaasFeatureService saasFeatureService;
    private final UserRepository userRepository;

    // ============================
    // FEATURE MANAGEMENT
    // ============================

    /**
     * Get all SaaS features with their current status
     */
    @GetMapping("/features")
    public ResponseEntity<List<SaasFeatureDTO>> getAllFeatures() {
        return ResponseEntity.ok(saasFeatureService.getAllFeatures());
    }

    /**
     * Get only active features (used by other modules to check access)
     */
    @GetMapping("/features/active")
    public ResponseEntity<List<SaasFeatureDTO>> getActiveFeatures() {
        return ResponseEntity.ok(saasFeatureService.getActiveFeatures());
    }

    /**
     * Request OTP to activate/deactivate a feature.
     * Sends 4-digit code to admin email.
     */
    @PostMapping("/features/request-toggle")
    public ResponseEntity<Map<String, String>> requestFeatureToggle(
            @RequestBody FeatureToggleRequest request) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(saasFeatureService.requestFeatureToggle(request, adminId));
    }

    /**
     * Request OTP to activate/deactivate multiple features with one verification.
     */
    @PostMapping("/features/request-toggle-all")
    public ResponseEntity<Map<String, String>> requestBulkFeatureToggle(
            @RequestBody BulkFeatureToggleRequest request) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(saasFeatureService.requestBulkFeatureToggle(request, adminId));
    }

    /**
     * Verify the OTP code and complete the feature toggle.
     * Code is validated by multiplying with 2003*9*23 and comparing.
     */
    @PostMapping("/features/verify-toggle")
    public ResponseEntity<SaasFeatureDTO> verifyAndToggleFeature(
            @RequestBody FeatureVerifyRequest request) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(saasFeatureService.verifyAndToggleFeature(request, adminId));
    }

    /**
     * Verify OTP and apply bulk feature toggle.
     */
    @PostMapping("/features/verify-toggle-all")
    public ResponseEntity<List<SaasFeatureDTO>> verifyAndToggleFeatures(
            @RequestBody BulkFeatureVerifyRequest request) {
        Long adminId = getCurrentUserId();
        return ResponseEntity.ok(saasFeatureService.verifyAndToggleFeatures(request, adminId));
    }

    // ============================
    // SYSTEM SETTINGS
    // ============================

    /**
     * Get system name
     */
    @GetMapping("/settings/system-name")
    public ResponseEntity<Map<String, String>> getSystemName() {
        Map<String, String> response = Map.of("systemName", saasFeatureService.getSystemName());
        return ResponseEntity.ok(response);
    }

    /**
     * Update system name (admin only)
     */
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

    /**
     * Get all system settings (public endpoint for all modules)
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(saasFeatureService.getAllSettings());
    }

    // ============================
    // HELPER
    // ============================

    /**
     * Extract current admin user ID from Spring Security context
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            String username = ((UserDetails) auth.getPrincipal()).getUsername();
            UserProfile user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                return user.getUserId();
            }
        }
        return 1L; // Fallback for dev
    }
}

