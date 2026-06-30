package com.nsbm.rocs.modules.admin.service;

import com.nsbm.rocs.modules.admin.dto.*;

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
}

