package com.silverline.erp.module.admin.service.impl;

import com.silverline.erp.infrastructure.email.EmailService;
import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.common.exception.ValidationException;
import com.silverline.erp.domain.system.FeatureVerificationCode;
import com.silverline.erp.domain.system.SaasFeature;
import com.silverline.erp.domain.system.SystemSetting;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.dto.*;
import com.silverline.erp.module.admin.repository.FeatureVerificationCodeRepository;
import com.silverline.erp.module.admin.repository.SaasFeatureRepository;
import com.silverline.erp.module.admin.repository.SystemSettingRepository;
import com.silverline.erp.module.admin.service.SaasFeatureService;
import com.silverline.erp.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class SaasFeatureServiceImpl implements SaasFeatureService {

    // Verification multiplier: 2003 * 9 * 23 = 414,621
    private static final long VERIFICATION_MULTIPLIER = 2003L * 9L * 23L;

    // OTP expires in 10 minutes
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final String BULK_FEATURE_CODE = "__BULK_FEATURE_TOGGLE__";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SaasFeatureRepository featureRepository;
    private final FeatureVerificationCodeRepository verificationCodeRepository;
    private final SystemSettingRepository settingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ================================================================
    // FEATURE MANAGEMENT
    // ================================================================

    @Override
    public List<SaasFeatureDTO> getAllFeatures() {
        return featureRepository.findAllByOrderByFeatureCategoryAscFeatureNameAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SaasFeatureDTO> getActiveFeatures() {
        return featureRepository.findByIsActiveTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ================================================================
    // OTP REQUEST â€” Generate code, send to admin email
    // ================================================================

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Map<String, String> requestFeatureToggle(FeatureToggleRequest request, Long adminUserId) {
        // 1. Validate feature exists
        SaasFeature feature = featureRepository.findByFeatureCode(request.getFeatureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found"));

        // 2. Validate action makes sense
        String action = request.getAction().toUpperCase();
        if ("ACTIVATE".equals(action) && Boolean.TRUE.equals(feature.getIsActive())) {
            throw new ValidationException("Feature is already active");
        }
        if ("DEACTIVATE".equals(action) && !Boolean.TRUE.equals(feature.getIsActive())) {
            throw new ValidationException("Feature is already inactive");
        }

        // 3. Generate random 4-digit code (1000-9999)
        Random random = new Random();
        int code = 1000 + random.nextInt(9000);

        // 4. Compute hashed code = code * 414621
        long hashedCode = (long) code * VERIFICATION_MULTIPLIER;

        // 5. Save verification code
        FeatureVerificationCode verificationCode = new FeatureVerificationCode();
        verificationCode.setFeatureCode(request.getFeatureCode());
        verificationCode.setAction(action);
        verificationCode.setVerificationCode(code);
        verificationCode.setHashedCode(hashedCode);
        verificationCode.setIsUsed(false);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        verificationCodeRepository.save(verificationCode);

        // 6. Get admin email
        UserProfile admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        String adminEmail = admin.getEmail();
        if (adminEmail == null || adminEmail.isEmpty()) {
            throw new ValidationException("Admin email not configured");
        }

        // 7. Get system name for branding
        String systemName = getSystemName();

        // 8. Send email
        String subject = systemName + " â€” Feature " + action + " Verification";
        String body = buildVerificationEmail(systemName, feature.getFeatureName(), action, code, admin.getFullName());

        try {
            emailService.sendSimpleMessage(adminEmail, subject, body);
        } catch (Exception e) {
            throw new ValidationException("Failed to send verification email. Please check email configuration.");
        }

        // 9. Return success message
        Map<String, String> response = new HashMap<>();
        response.put("message", "Verification code sent to " + maskEmail(adminEmail));
        response.put("featureCode", request.getFeatureCode());
        response.put("action", action);
        return response;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Map<String, String> requestBulkFeatureToggle(BulkFeatureToggleRequest request, Long adminUserId) {
        String action = normalizeAction(request.getAction());
        List<SaasFeature> targetFeatures = resolveTargetFeatures(request.getFeatureCodes(), action);

        if (targetFeatures.isEmpty()) {
            throw new ValidationException("No matching features for bulk toggle");
        }

        int code = generateVerificationCode();
        long hashedCode = (long) code * VERIFICATION_MULTIPLIER;

        FeatureVerificationCode verificationCode = new FeatureVerificationCode();
        verificationCode.setFeatureCode(BULK_FEATURE_CODE);
        verificationCode.setAction(action);
        verificationCode.setVerificationCode(code);
        verificationCode.setHashedCode(hashedCode);
        verificationCode.setIsUsed(false);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        verificationCodeRepository.save(verificationCode);

        UserProfile admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        String adminEmail = admin.getEmail();
        if (adminEmail == null || adminEmail.isEmpty()) {
            throw new ValidationException("Admin email not configured");
        }

        String systemName = getSystemName();
        String subject = systemName + " â€” Feature " + action + " Verification";
        String featureLabel = targetFeatures.size() + " premium feature" + (targetFeatures.size() > 1 ? "s" : "");
        String body = buildVerificationEmail(systemName, featureLabel, action, code, admin.getFullName());

        try {
            emailService.sendSimpleMessage(adminEmail, subject, body);
        } catch (Exception e) {
            throw new ValidationException("Failed to send verification email. Please check email configuration.");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Verification code sent to " + maskEmail(adminEmail));
        response.put("action", action);
        response.put("count", String.valueOf(targetFeatures.size()));
        return response;
    }

    // ================================================================
    // OTP VERIFY â€” Validate code x 414621, toggle feature
    // ================================================================

    @Override
    @org.springframework.transaction.annotation.Transactional
    public SaasFeatureDTO verifyAndToggleFeature(FeatureVerifyRequest request, Long adminUserId) {
        // 1. Validate feature exists
        SaasFeature feature = featureRepository.findByFeatureCode(request.getFeatureCode())
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found"));

        String action = request.getAction().toUpperCase();

        // 2. Find the latest unused verification code
        FeatureVerificationCode storedCode = verificationCodeRepository
                .findTopByFeatureCodeAndActionAndIsUsedFalseOrderByCreatedAtDesc(
                        request.getFeatureCode(), action)
                .orElseThrow(() -> new ValidationException(
                        "No pending verification found. Please request a new code."));

        // 3. Check expiry
        if (storedCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedCode.setIsUsed(true);
            verificationCodeRepository.save(storedCode);
            throw new ValidationException(
                    "Verification code has expired. Please request a new one.");
        }

        // 4. Compare entered verification key directly with stored hashed code
        if (!request.getVerificationKey().equals(storedCode.getHashedCode())) {
            throw new ValidationException("Invalid verification code");
        }

        // 5. Mark code as used
        storedCode.setIsUsed(true);
        verificationCodeRepository.save(storedCode);

        // 6. Toggle feature
        if ("ACTIVATE".equals(action)) {
            feature.setIsActive(true);
            feature.setActivatedAt(LocalDateTime.now());
            feature.setDeactivatedAt(null);
        } else {
            feature.setIsActive(false);
            feature.setDeactivatedAt(LocalDateTime.now());
        }
        feature.setActivatedBy(adminUserId);
        featureRepository.save(feature);

        return toDTO(feature);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public List<SaasFeatureDTO> verifyAndToggleFeatures(BulkFeatureVerifyRequest request, Long adminUserId) {
        String action = normalizeAction(request.getAction());
        List<SaasFeature> targetFeatures = resolveTargetFeatures(request.getFeatureCodes(), action);

        if (targetFeatures.isEmpty()) {
            throw new ValidationException("No matching features for bulk toggle");
        }

        FeatureVerificationCode storedCode = verificationCodeRepository
                .findTopByFeatureCodeAndActionAndIsUsedFalseOrderByCreatedAtDesc(BULK_FEATURE_CODE, action)
                .orElseThrow(() -> new ValidationException(
                        "No pending verification found. Please request a new code."));

        if (storedCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedCode.setIsUsed(true);
            verificationCodeRepository.save(storedCode);
            throw new ValidationException(
                    "Verification code has expired. Please request a new one.");
        }

        if (request.getVerificationKey() == null || !request.getVerificationKey().equals(storedCode.getHashedCode())) {
            throw new ValidationException("Invalid verification code");
        }

        storedCode.setIsUsed(true);
        verificationCodeRepository.save(storedCode);

        LocalDateTime now = LocalDateTime.now();
        List<SaasFeatureDTO> updated = new ArrayList<>();
        for (SaasFeature feature : targetFeatures) {
            if ("ACTIVATE".equals(action)) {
                feature.setIsActive(true);
                feature.setActivatedAt(now);
                feature.setDeactivatedAt(null);
            } else {
                feature.setIsActive(false);
                feature.setDeactivatedAt(now);
            }
            feature.setActivatedBy(adminUserId);
            featureRepository.save(feature);
            updated.add(toDTO(feature));
        }

        return updated;
    }

    // ================================================================
    // SYSTEM SETTINGS
    // ================================================================

    @Override
    public String getSystemName() {
        return settingRepository.findBySettingKey("SYSTEM_NAME")
                .map(SystemSetting::getSettingValue)
                .orElse("SmartRetail Pro");
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateSystemName(String newName, Long adminUserId) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("System name cannot be empty");
        }

        SystemSetting setting = settingRepository.findBySettingKey("SYSTEM_NAME")
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setSettingKey("SYSTEM_NAME");
                    return s;
                });

        setting.setSettingValue(newName.trim());
        setting.setUpdatedBy(adminUserId);
        settingRepository.save(setting);
    }

    @Override
    public Map<String, String> getAllSettings() {
        Map<String, String> settings = new HashMap<>();
        settingRepository.findAll().forEach(s -> settings.put(s.getSettingKey(), s.getSettingValue()));
        return settings;
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private SaasFeatureDTO toDTO(SaasFeature f) {
        SaasFeatureDTO dto = new SaasFeatureDTO();
        dto.setId(f.getId());
        dto.setFeatureCode(f.getFeatureCode());
        dto.setFeatureName(f.getFeatureName());
        dto.setFeatureCategory(f.getFeatureCategory());
        dto.setIsActive(f.getIsActive());
        dto.setActivatedAt(f.getActivatedAt() != null ? f.getActivatedAt().format(DATE_FMT) : null);
        dto.setDeactivatedAt(f.getDeactivatedAt() != null ? f.getDeactivatedAt().format(DATE_FMT) : null);

        // Resolve activated_by user name
        if (f.getActivatedBy() != null) {
            try {
                UserProfile user = userRepository.findById(f.getActivatedBy()).orElse(null);
                if (user != null) {
                    dto.setActivatedByName(user.getFullName());
                }
            } catch (Exception ignored) {}
        }

        return dto;
    }

    private int generateVerificationCode() {
        Random random = new Random();
        return 1000 + random.nextInt(9000);
    }

    private String normalizeAction(String rawAction) {
        if (rawAction == null) {
            throw new ValidationException("Action is required");
        }
        String action = rawAction.toUpperCase(Locale.ROOT);
        if (!"ACTIVATE".equals(action) && !"DEACTIVATE".equals(action)) {
            throw new ValidationException("Action must be ACTIVATE or DEACTIVATE");
        }
        return action;
    }

    private List<SaasFeature> resolveTargetFeatures(List<String> featureCodes, String action) {
        if (featureCodes == null || featureCodes.isEmpty()) {
            throw new ValidationException("Feature list cannot be empty");
        }

        List<SaasFeature> features = featureCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .distinct()
                .map(code -> featureRepository.findByFeatureCode(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + code)))
                .collect(Collectors.toList());

        for (SaasFeature feature : features) {
            if (!"PREMIUM".equalsIgnoreCase(feature.getFeatureCategory())) {
                throw new ValidationException("Bulk toggle is only allowed for premium features");
            }

            boolean isActive = Boolean.TRUE.equals(feature.getIsActive());
            if ("ACTIVATE".equals(action) && isActive) {
                throw new ValidationException("Feature is already active: " + feature.getFeatureCode());
            }
            if ("DEACTIVATE".equals(action) && !isActive) {
                throw new ValidationException("Feature is already inactive: " + feature.getFeatureCode());
            }
        }

        return features;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 3) {
            return local.charAt(0) + "***@" + parts[1];
        }
        return local.substring(0, 3) + "***@" + parts[1];
    }

    private String buildVerificationEmail(String systemName, String featureName,
                                           String action, int code, String adminName) {
        return String.format(
                "Hello %s,\n\n" +
                "You have requested to %s the feature \"%s\" on %s.\n\n" +
                "Your verification code is:\n\n" +
                "    %d\n\n" +
                "This code will expire in %d minutes.\n\n" +
                "If you did not request this action, please ignore this email.\n\n" +
                "— %s System",
                adminName,
                action.toLowerCase(),
                featureName,
                systemName,
                code,
                OTP_EXPIRY_MINUTES,
                systemName
        );
    }

    @Override
    public boolean isFeatureEnabled(String featureCode) {
        return featureRepository.findByFeatureCode(featureCode)
                .map(SaasFeature::getIsActive)
                .orElse(false);
    }
}
