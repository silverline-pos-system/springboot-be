package com.silverline.erp.module.notification.controller;

import com.silverline.erp.domain.notification.Notification;
import com.silverline.erp.domain.notification.NotificationRecipient;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.common.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

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

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return ((UserProfile) auth.getPrincipal()).getUserId();
        }
        throw new RuntimeException("Not authenticated");
    }
}


