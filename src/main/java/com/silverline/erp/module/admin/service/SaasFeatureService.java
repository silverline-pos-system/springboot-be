package com.silverline.erp.module.admin.service;

import com.silverline.erp.module.admin.dto.BulkFeatureToggleRequest;
import com.silverline.erp.module.admin.dto.BulkFeatureVerifyRequest;
import com.silverline.erp.module.admin.dto.FeatureToggleRequest;
import com.silverline.erp.module.admin.dto.FeatureVerifyRequest;
import com.silverline.erp.module.admin.dto.SaasFeatureDTO;
import com.silverline.erp.module.admin.dto.*;

import java.util.List;
import java.util.Map;

public interface SaasFeatureService {

    // Feature management
    List<SaasFeatureDTO> getAllFeatures();

    List<SaasFeatureDTO> getActiveFeatures();

    // Request OTP for feature toggle (sends email)
    Map<String, String> requestFeatureToggle(FeatureToggleRequest request, Long adminUserId);

    // Request OTP for bulk feature toggle (single email verification)
    Map<String, String> requestBulkFeatureToggle(BulkFeatureToggleRequest request, Long adminUserId);

    // Verify OTP code and toggle feature
    SaasFeatureDTO verifyAndToggleFeature(FeatureVerifyRequest request, Long adminUserId);

    // Verify OTP code and toggle multiple features
    List<SaasFeatureDTO> verifyAndToggleFeatures(BulkFeatureVerifyRequest request, Long adminUserId);

    // System settings
    String getSystemName();

    void updateSystemName(String newName, Long adminUserId);

    Map<String, String> getAllSettings();

    // Check if feature is enabled
    boolean isFeatureEnabled(String featureCode);
}

