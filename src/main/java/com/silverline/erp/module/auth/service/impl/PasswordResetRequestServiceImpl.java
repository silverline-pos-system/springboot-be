package com.silverline.erp.module.auth.service.impl;

import com.silverline.erp.common.audit.repository.PasswordResetRequestRepository;
import com.silverline.erp.domain.audit.PasswordResetRequest;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.admin.event.PasswordResetCountChangedEvent;
import com.silverline.erp.module.auth.service.PasswordResetRequestService;
import com.silverline.erp.infrastructure.email.EmailService;
import com.silverline.erp.infrastructure.email.TemplateEngine;
import com.silverline.erp.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetRequestServiceImpl implements PasswordResetRequestService {

    private final UserProfileRepository userProfileRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    @Override
    @Transactional
    public void forgotPassword(String username, String newPassword, String reason) {
        Optional<UserProfile> userOpt = userProfileRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new com.silverline.erp.common.exception.ResourceNotFoundException("Username not found. Please check and try again.");
        }
        UserProfile user = userOpt.get();

        List<PasswordResetRequest> existing = passwordResetRequestRepository.findByUserId(user.getUserId());
        boolean hasPending = existing.stream().anyMatch(r -> "PENDING".equals(r.getStatus()) || "VERIFIED".equals(r.getStatus()));
        if (hasPending) {
            throw new com.silverline.erp.common.exception.ValidationException("You already have a pending or verified password reset request. Please wait for admin approval.");
        }

        // Generate a 6-digit verification code
        String token = String.format("%06d", new SecureRandom().nextInt(1000000));
        String tokenHash = SecurityUtils.hashToken(token);

        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setUserId(user.getUserId());
        resetRequest.setUsername(user.getUsername());
        resetRequest.setFullName(user.getFullName());
        resetRequest.setEmail(user.getEmail());
        resetRequest.setNewPasswordHash(passwordEncoder.encode(newPassword));
        resetRequest.setTokenHash(tokenHash);
        resetRequest.setStatus("PENDING");
        resetRequest.setRequestNotes(reason != null ? reason : "Password reset requested");

        passwordResetRequestRepository.save(resetRequest);

        // Publish event to broadcast the updated pending count asynchronously
        eventPublisher.publishEvent(new PasswordResetCountChangedEvent(this));

        // Send email with verification code
        try {
            String subject = "Password Reset Verification - Silverline";
            String htmlContent = TemplateEngine.loadAndResolve(
                    "password_reset_request",
                    Map.of(
                            "fullName", user.getFullName(),
                            "username", user.getUsername(),
                            "verificationCode", token
                    )
            );
            emailService.sendHtmlMessage(user.getEmail(), subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send password reset verification email: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void verifyForgotPasswordToken(String username, String token) {
        Optional<UserProfile> userOpt = userProfileRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new com.silverline.erp.common.exception.ResourceNotFoundException("Username not found.");
        }
        UserProfile user = userOpt.get();

        List<PasswordResetRequest> requests = passwordResetRequestRepository.findByUserId(user.getUserId());
        PasswordResetRequest pendingRequest = requests.stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .findFirst()
                .orElseThrow(() -> new com.silverline.erp.common.exception.ValidationException("No pending password reset request found."));

        String inputHash = SecurityUtils.hashToken(token);
        if (!inputHash.equals(pendingRequest.getTokenHash())) {
            throw new com.silverline.erp.common.exception.ValidationException("Invalid verification code. Please try again.");
        }

        pendingRequest.setStatus("VERIFIED");
        passwordResetRequestRepository.save(pendingRequest);

        // Publish event to broadcast the updated pending count asynchronously
        eventPublisher.publishEvent(new PasswordResetCountChangedEvent(this));
    }
}
