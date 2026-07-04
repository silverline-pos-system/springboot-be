package com.silverline.erp.module.admin.controller;

import com.silverline.erp.common.audit.repository.PasswordResetRequestRepository;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.infrastructure.email.EmailService;
import com.silverline.erp.domain.audit.PasswordResetRequest;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.dto.PasswordResetResponseDTO;
import com.silverline.erp.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/password-requests")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * GET /api/v1/admin/password-requests
     * Get all password reset requests (sorted newest first)
     * All managers see all requests â€” no branch filtering
     */
    @GetMapping("")
    public ResponseEntity<List<PasswordResetResponseDTO>> getAllRequests(
            @RequestParam(required = false) String status) {

        // NOTE: No branch filtering â€” all managers/admins see all requests
        List<PasswordResetRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = passwordResetRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            requests = passwordResetRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        return ResponseEntity.ok(requests.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * GET /api/v1/admin/password-requests/count
     * Get count of pending password reset requests
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        // NOTE: No branch filtering â€” all managers see all pending counts
        long count = passwordResetRequestRepository.countByStatus("PENDING");
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    /**
     * PATCH /api/v1/admin/password-requests/{id}/approve
     * Approve a password reset request - applies the new password
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body) {
        PasswordResetRequest request = passwordResetRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "This request has already been " + request.getStatus().toLowerCase()));
        }

        // Find the user and apply the new password
        UserProfile user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Apply the pre-hashed password
        user.setPassword(request.getNewPasswordHash());
        userRepository.save(user);

        // Update request status
        request.setStatus("APPROVED");
        request.setReviewedBy(SecurityUtils.getCurrentUserId());
        request.setReviewedAt(LocalDateTime.now());
        if (body != null && body.containsKey("adminNotes")) {
            request.setAdminNotes(body.get("adminNotes"));
        }
        passwordResetRequestRepository.save(request);

        // Send email notification to user
        try {
            String subject = "Password Reset Approved - Silverline";
            String emailBody = "Dear " + request.getFullName() + ",\n\n" +
                    "Your password reset request has been approved by the administrator.\n" +
                    "You can now log in with your new password.\n\n" +
                    "If you did not request this change, please contact your administrator immediately.\n\n" +
                    "Best Regards,\n" +
                    "Silverline Admin Team";
            emailService.sendSimpleMessage(request.getEmail(), subject, emailBody);
        } catch (Exception e) {
            log.error("Failed to send password reset approval email to {}: {}", request.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Password reset approved successfully. User has been notified."));
    }

    /**
     * PATCH /api/v1/admin/password-requests/{id}/reject
     * Reject a password reset request
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        PasswordResetRequest request = passwordResetRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "This request has already been " + request.getStatus().toLowerCase()));
        }

        // Update request status
        request.setStatus("REJECTED");
        request.setReviewedBy(SecurityUtils.getCurrentUserId());
        request.setReviewedAt(LocalDateTime.now());
        if (body != null && body.containsKey("adminNotes")) {
            request.setAdminNotes(body.get("adminNotes"));
        }
        passwordResetRequestRepository.save(request);

        // Send email notification to user
        try {
            String reason = (body != null && body.containsKey("adminNotes")) ? body.get("adminNotes") : "No reason provided";
            String subject = "Password Reset Request Rejected - Silverline";
            String emailBody = "Dear " + request.getFullName() + ",\n\n" +
                    "Your password reset request has been rejected by the administrator.\n" +
                    "Reason: " + reason + "\n\n" +
                    "If you believe this is an error, please contact your administrator.\n\n" +
                    "Best Regards,\n" +
                    "Silverline Admin Team";
            emailService.sendSimpleMessage(request.getEmail(), subject, emailBody);
        } catch (Exception e) {
            log.error("Failed to send password reset rejection email to {}: {}", request.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Password reset request rejected."));
    }

    private PasswordResetResponseDTO toDTO(PasswordResetRequest request) {
        PasswordResetResponseDTO dto = new PasswordResetResponseDTO();
        dto.setId(request.getId());
        dto.setUserId(request.getUserId());
        dto.setUsername(request.getUsername());
        dto.setFullName(request.getFullName());
        dto.setEmail(request.getEmail());
        dto.setStatus(request.getStatus());
        dto.setRequestNotes(request.getRequestNotes());
        dto.setAdminNotes(request.getAdminNotes());
        dto.setReviewedBy(request.getReviewedBy());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setReviewedAt(request.getReviewedAt());
        return dto;
    }
}


