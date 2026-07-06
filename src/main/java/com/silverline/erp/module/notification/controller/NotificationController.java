package com.silverline.erp.module.notification.controller;

import com.silverline.erp.domain.notification.NotificationRecipient;
import com.silverline.erp.infrastructure.sse.SseEmitterRegistry;
import com.silverline.erp.module.notification.service.NotificationService;
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
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * GET /api/v1/notifications
     * Paginated list of notifications for the current user
     */
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

    /**
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long userId = getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * PUT /api/v1/notifications/{notificationId}/read
     * Mark a specific notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long notificationId) {
        Long userId = getCurrentUserId();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    /**
     * PUT /api/v1/notifications/read-all
     * Mark all notifications as read for current user
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    /**
     * GET /api/v1/notifications/stream
     * Establish a Server-Sent Events stream for the current authenticated user.
     */
    @GetMapping("/stream")
    public SseEmitter streamNotifications() {
        Long userId = getCurrentUserId();
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout
        sseEmitterRegistry.register(userId, emitter);

        // Send connection initialization event
        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("connected"));
        } catch (Exception e) {
            // registry's onError will handle cleaning up the emitter
        }

        return emitter;
    }

    private Long getCurrentUserId() {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("Not authenticated");
        }
        return userId;
    }
}


