package com.silverline.erp.module.admin.controller;

import com.silverline.erp.common.audit.repository.PasswordResetRequestRepository;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.domain.audit.PasswordResetRequest;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.infrastructure.email.EmailService;
import com.silverline.erp.infrastructure.sse.SseChannel;
import com.silverline.erp.infrastructure.sse.SseEmitterRegistry;
import com.silverline.erp.module.admin.dto.PasswordResetResponseDTO;
import com.silverline.erp.module.admin.event.PasswordResetCountChangedEvent;
import com.silverline.erp.module.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
@Tag(name = "Password Reset Requests", description = "APIs for administrators and managers to audit, approve, or reject user password reset requests")
public class PasswordResetController {

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Operation(summary = "Get all password reset requests", description = "Lists all password reset requests in the system, sorted by creation date (newest first). Can filter by status.")
    @ApiResponse(responseCode = "200", description = "Requests list fetched successfully")
    @GetMapping("")
    public ResponseEntity<List<PasswordResetResponseDTO>> getAllRequests(
            @RequestParam(required = false) String status) {

        List<PasswordResetRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = passwordResetRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            requests = passwordResetRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        return ResponseEntity.ok(requests.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @Operation(summary = "Get pending requests count", description = "Retrieves the total count of pending password reset requests awaiting verification/approval")
    @ApiResponse(responseCode = "200", description = "Pending count retrieved successfully")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        long count = passwordResetRequestRepository.countByStatus("PENDING");
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    @Operation(summary = "Approve password reset request", description = "Approve a verified request, applying the pre-hashed new password to the user profile and notifying the user via email")
    @ApiResponse(responseCode = "200", description = "Password reset request approved successfully")
    @ApiResponse(responseCode = "400", description = "Request is not in VERIFIED status and cannot be approved")
    @ApiResponse(responseCode = "404", description = "Request or User profile not found")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body) {
        PasswordResetRequest request = passwordResetRequestRepository.findById(id)
                .orElseThrow(() -> new com.silverline.erp.common.exception.ResourceNotFoundException("Request not found"));

        if (!"VERIFIED".equals(request.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Only verified password reset requests can be approved. Current status: " + request.getStatus()));
        }

        UserProfile user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new com.silverline.erp.common.exception.ResourceNotFoundException("User not found"));

        user.setPassword(request.getNewPasswordHash());
        userRepository.save(user);

        request.setStatus("APPROVED");
        request.setReviewedBy(SecurityUtils.getCurrentUserId());
        request.setReviewedAt(LocalDateTime.now());
        if (body != null && body.containsKey("adminNotes")) {
            request.setAdminNotes(body.get("adminNotes"));
        }
        passwordResetRequestRepository.save(request);

        eventPublisher.publishEvent(new PasswordResetCountChangedEvent(this));

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

    @Operation(summary = "Reject password reset request", description = "Rejects a pending or verified password reset request, logging the rejection reason and notifying the user via email")
    @ApiResponse(responseCode = "200", description = "Password reset request rejected successfully")
    @ApiResponse(responseCode = "400", description = "Request is not in PENDING or VERIFIED status")
    @ApiResponse(responseCode = "404", description = "Request not found")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        PasswordResetRequest request = passwordResetRequestRepository.findById(id)
                .orElseThrow(() -> new com.silverline.erp.common.exception.ResourceNotFoundException("Request not found"));

        if (!"PENDING".equals(request.getStatus()) && !"VERIFIED".equals(request.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Only pending or verified requests can be rejected. Current status: " + request.getStatus()));
        }

        request.setStatus("REJECTED");
        request.setReviewedBy(SecurityUtils.getCurrentUserId());
        request.setReviewedAt(LocalDateTime.now());
        if (body != null && body.containsKey("adminNotes")) {
            request.setAdminNotes(body.get("adminNotes"));
        }
        passwordResetRequestRepository.save(request);

        eventPublisher.publishEvent(new PasswordResetCountChangedEvent(this));

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

    @Operation(summary = "Subscribe to pending count stream", description = "Establishes a real-time Server-Sent Events (SSE) stream to receive live pending request count updates")
    @ApiResponse(responseCode = "200", description = "SSE stream connection established")
    @GetMapping("/stream")
    public SseEmitter streamPasswordRequests() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new com.silverline.erp.common.exception.UnauthorizedException("Not authenticated");
        }
        SseEmitter emitter = new SseEmitter(180_000L);
        sseEmitterRegistry.register(SseChannel.PASSWORD_RESETS, userId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("connected"));
            
            long initialCount = passwordResetRequestRepository.countByStatus("PENDING");
            emitter.send(SseEmitter.event()
                    .data(Map.of("pendingCount", initialCount)));
        } catch (Exception e) {
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                // Ignore
            }
        }

        return emitter;
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
