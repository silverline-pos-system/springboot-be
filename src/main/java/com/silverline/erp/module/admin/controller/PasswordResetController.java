package com.silverline.erp.module.admin.controller;

import com.silverline.erp.common.audit.repository.PasswordResetRequestRepository;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.domain.audit.PasswordResetRequest;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.infrastructure.email.EmailService;
import com.silverline.erp.infrastructure.sse.SseEmitterRegistry;
import com.silverline.erp.module.admin.dto.PasswordResetResponseDTO;
import com.silverline.erp.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final SseEmitterRegistry sseEmitterRegistry;

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
                .orElseThrow(() -> new com.silverline.erp.common.exception.ResourceNotFoundException("Request not found"));

        if (!"VERIFIED".equals(request.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Only verified password reset requests can be approved. Current status: " + request.getStatus()));
        }

        // Find the user and apply the new password
        UserProfile user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new com.silverline.erp.common.exception.ResourceNotFoundException("User not found"));

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

        // Broadcast updated count to active managers
        broadcastPendingCount();

        // Send email notification to user
        try {
            String subject = "Password Reset Approved - Silverline";
            String htmlContent = com.silverline.erp.infrastructure.email.TemplateEngine.loadAndResolve(
                    "password_reset_approved",
                    Map.of("fullName", request.getFullName())
            );
            emailService.sendHtmlMessage(request.getEmail(), subject, htmlContent);
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
                .orElseThrow(() -> new com.silverline.erp.common.exception.ResourceNotFoundException("Request not found"));

        if (!"PENDING".equals(request.getStatus()) && !"VERIFIED".equals(request.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Only pending or verified requests can be rejected. Current status: " + request.getStatus()));
        }

        // Update request status
        request.setStatus("REJECTED");
        request.setReviewedBy(SecurityUtils.getCurrentUserId());
        request.setReviewedAt(LocalDateTime.now());
        if (body != null && body.containsKey("adminNotes")) {
            request.setAdminNotes(body.get("adminNotes"));
        }
        passwordResetRequestRepository.save(request);

        // Broadcast updated count to active managers
        broadcastPendingCount();

        // Send email notification to user
        try {
            String reason = (body != null && body.containsKey("adminNotes")) ? body.get("adminNotes") : "No reason provided";
            String subject = "Password Reset Request Rejected - Silverline";
            String htmlContent = com.silverline.erp.infrastructure.email.TemplateEngine.loadAndResolve(
                    "password_reset_rejected",
                    Map.of(
                            "fullName", request.getFullName(),
                            "reason", reason
                    )
            );
            emailService.sendHtmlMessage(request.getEmail(), subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send password reset rejection email to {}: {}", request.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Password reset request rejected."));
    }

    /**
     * GET /api/v1/admin/password-requests/stream
     * SSE endpoint for real-time password reset request updates.
     * Managers subscribe here to receive live notifications when new requests arrive.
     */
    @GetMapping("/stream")
    public SseEmitter streamPasswordRequests() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new com.silverline.erp.common.exception.UnauthorizedException("Not authenticated");
        }
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout
        sseEmitterRegistry.register(userId, emitter);

        // Send initial connection event and current count
        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("connected"));
            
            long initialCount = passwordResetRequestRepository.countByStatus("PENDING");
            emitter.send(SseEmitter.event()
                    .data(Map.of("pendingCount", initialCount)));
        } catch (Exception e) {
            // registry's onError callback handles cleanup
        }

        return emitter;
    }

    private void broadcastPendingCount() {
        try {
            long count = passwordResetRequestRepository.countByStatus("PENDING");
            sseEmitterRegistry.broadcast(Map.of("pendingCount", count));
        } catch (Exception e) {
            log.error("Failed to broadcast updated pending count: {}", e.getMessage());
        }
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


