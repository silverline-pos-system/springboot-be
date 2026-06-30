package com.nsbm.rocs.shared;

import com.nsbm.rocs.modules.admin.dto.SaasFeatureDTO;
import com.nsbm.rocs.modules.admin.service.SaasFeatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public-facing controller for system configuration.
 * Used by all modules (POS, Inventory, Manager) to read system name and active features.
 * No admin auth required â€” read-only access.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/v1/system")
public class SystemConfigController {

    private final SaasFeatureService saasFeatureService;

    @Autowired
    public SystemConfigController(SaasFeatureService saasFeatureService) {
        this.saasFeatureService = saasFeatureService;
    }

    /**
     * Get the system name (used by all layouts, headers, receipts, reports)
     */
    @GetMapping("/name")
    public ResponseEntity<Map<String, String>> getSystemName() {
        return ResponseEntity.ok(Map.of("systemName", saasFeatureService.getSystemName()));
    }

    /**
     * Get all features with their status (used by frontend FeatureGate)
     */
    @GetMapping("/features")
    public ResponseEntity<List<SaasFeatureDTO>> getAllFeatures() {
        return ResponseEntity.ok(saasFeatureService.getAllFeatures());
    }

    /**
     * Get only active features
     */
    @GetMapping("/features/active")
    public ResponseEntity<List<SaasFeatureDTO>> getActiveFeatures() {
        return ResponseEntity.ok(saasFeatureService.getActiveFeatures());
    }

    /**
     * Get all system settings
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(saasFeatureService.getAllSettings());
    }
}


