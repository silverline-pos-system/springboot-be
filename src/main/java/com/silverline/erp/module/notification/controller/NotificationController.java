package com.silverline.erp.module.notification.controller;

import com.silverline.erp.domain.notification.NotificationRecipient;
import com.silverline.erp.infrastructure.sse.SseChannel;
import com.silverline.erp.infrastructure.sse.SseEmitterRegistry;
import com.silverline.erp.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Real-Time Notification Subscriptions", description = "APIs for users to retrieve, mark read, and subscribe to real-time notification events (SSE)")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Operation(summary = "Get user notifications list", description = "Retrieves a paginated list of notifications for the current authenticated user")
    @ApiResponse(responseCode = "200", description = "Notifications list retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationRecipient> recipients = notificationService.getNotificationsForUser(userId, pageable);

        Page<Map<String, Object>> result = recipients.map(nr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", nr.getId());
            map.put("notificationId", nr.getNotification().getNotificationId());
            map.put("type", nr.getNotification().getType());
            map.put("title", nr.getNotification().getTitle());
            map.put("message", nr.getNotification().getMessage());
            map.put("referenceType", nr.getNotification().getReferenceType());
            map.put("referenceId", nr.getNotification().getReferenceId());
            map.put("priority", nr.getNotification().getPriority());
            map.put("isRead", nr.getIsRead());
            map.put("readAt", nr.getReadAt());
            map.put("createdAt", nr.getNotification().getCreatedAt());
            return map;
        });

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get unread notifications count", description = "Retrieves total count of unread notifications for the current user")
    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long userId = getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "Mark notification as read", description = "Updates a notification status to read for a specific notification ID")
    @ApiResponse(responseCode = "200", description = "Notification marked as read successfully")
    @ApiResponse(responseCode = "404", description = "Notification not found for this user")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long notificationId) {
        Long userId = getCurrentUserId();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @Operation(summary = "Mark all notifications as read", description = "Updates all unread notifications to read for the current user context")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully")
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @Operation(summary = "Subscribe to live notification stream", description = "Establishes a Server-Sent Events (SSE) connection to receive real-time alerts (e.g. transfer requests)")
    @ApiResponse(responseCode = "200", description = "SSE connection established successfully")
    @GetMapping("/stream")
    public SseEmitter streamNotifications() {
        Long userId = getCurrentUserId();
        SseEmitter emitter = new SseEmitter(180_000L);
        sseEmitterRegistry.register(SseChannel.NOTIFICATIONS, userId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("connected"));
        } catch (Exception e) {
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                // Ignore
            }
        }

        return emitter;
    }

    private Long getCurrentUserId() {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new com.silverline.erp.common.exception.UnauthorizedException("Not authenticated");
        }
        return userId;
    }
}
