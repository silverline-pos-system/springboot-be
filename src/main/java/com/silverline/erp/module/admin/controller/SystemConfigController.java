package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.SaasFeatureDTO;
import com.silverline.erp.module.admin.service.SaasFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System Constants Configurations", description = "Public read-only APIs for fetching system branding parameters and active SaaS features (accessed by all users without admin auth)")
public class SystemConfigController {

    private final SaasFeatureService saasFeatureService;

    @Operation(summary = "Get branding system name", description = "Fetches the current brand/custom system name string (e.g. for page headers and receipts)")
    @ApiResponse(responseCode = "200", description = "System name retrieved successfully")
    @GetMapping("/name")
    public ResponseEntity<Map<String, String>> getSystemName() {
        return ResponseEntity.ok(Map.of("systemName", saasFeatureService.getSystemName()));
    }

    @Operation(summary = "Get all SaaS features configuration", description = "Retrieves a comprehensive list of all SaaS features and their status to control visibility of layout fields in client gates")
    @ApiResponse(responseCode = "200", description = "Features list retrieved successfully")
    @GetMapping("/features")
    public ResponseEntity<List<SaasFeatureDTO>> getAllFeatures() {
        return ResponseEntity.ok(saasFeatureService.getAllFeatures());
    }

    @Operation(summary = "Get only active SaaS features", description = "Retrieves active features that are currently enabled in the system")
    @ApiResponse(responseCode = "200", description = "Active features list retrieved successfully")
    @GetMapping("/features/active")
    public ResponseEntity<List<SaasFeatureDTO>> getActiveFeatures() {
        return ResponseEntity.ok(saasFeatureService.getActiveFeatures());
    }

    @Operation(summary = "Get all settings", description = "Fetches a map of all public key-value configurations configured in system settings")
    @ApiResponse(responseCode = "200", description = "Settings map retrieved successfully")
    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(saasFeatureService.getAllSettings());
    }
}
