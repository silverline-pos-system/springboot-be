package com.nsbm.rocs.shared.notifications;

import com.nsbm.rocs.entity.main.Notification;
import com.nsbm.rocs.entity.main.NotificationRecipient;
import com.nsbm.rocs.entity.main.UserProfile;
import com.nsbm.rocs.entity.enums.Role;
import com.nsbm.rocs.repository.NotificationRepository;
import com.nsbm.rocs.repository.NotificationRecipientRepository;
import com.nsbm.rocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a notification and broadcast it to ALL managers and admins.
     * This is the core method that implements the "ALL managers simultaneously" requirement.
     */
    @Transactional
    public Notification createAndBroadcast(String type, String title, String message,
                                            String referenceType, Long referenceId,
                                            String priority, Long createdBy) {
        // 1. Create the notification
        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .priority(priority != null ? priority : "NORMAL")
                .createdBy(createdBy)
                .build();
        notification = notificationRepository.save(notification);

        // 2. Find ALL managers, admins, and super admins
        List<UserProfile> recipients = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null &&
                        (u.getRole() == Role.MANAGER ||
                         u.getRole() == Role.SUPER_ADMIN))
                .filter(u -> u.getAccountStatus() != null &&
                        u.getAccountStatus().name().equals("ACTIVE"))
                .collect(Collectors.toList());

        // 3. Create recipient entries for each manager/admin
        for (UserProfile recipient : recipients) {
            NotificationRecipient nr = NotificationRecipient.builder()
                    .notification(notification)
                    .userId(recipient.getUserId())
                    .isRead(false)
                    .build();
            notificationRecipientRepository.save(nr);
        }

        // 4. Broadcast via WebSocket to /topic/manager-notifications
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("notificationId", notification.getNotificationId());
        wsPayload.put("type", notification.getType());
        wsPayload.put("title", notification.getTitle());
        wsPayload.put("message", notification.getMessage());
        wsPayload.put("referenceType", notification.getReferenceType());
        wsPayload.put("referenceId", notification.getReferenceId());
        wsPayload.put("priority", notification.getPriority());
        wsPayload.put("createdAt", notification.getCreatedAt());

        try {
            messagingTemplate.convertAndSend("/topic/manager-notifications", (Object) wsPayload);
            log.info("Broadcast notification '{}' to {} managers/admins", title, recipients.size());
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket notification: {}", e.getMessage());
        }

        return notification;
    }

    /**
     * Get paginated notifications for a specific user.
     */
    public Page<NotificationRecipient> getNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRecipientRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get unread notification count for a user.
     */
    public long getUnreadCount(Long userId) {
        return notificationRecipientRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRecipientRepository.markAsRead(notificationId, userId, LocalDateTime.now());
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRecipientRepository.markAllAsReadForUser(userId, LocalDateTime.now());
    }

    /**
     * Broadcast a dashboard update event via WebSocket.
     * Called when an approval action happens so OTHER managers see the update.
     */
    public void broadcastDashboardUpdate(String eventType, Map<String, Object> data) {
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("eventType", eventType);
        wsPayload.put("data", data);
        wsPayload.put("timestamp", LocalDateTime.now());

        try {
            messagingTemplate.convertAndSend("/topic/manager-dashboard", (Object) wsPayload);
            log.info("Broadcast dashboard update: {}", eventType);
        } catch (Exception e) {
            log.error("Failed to broadcast dashboard update: {}", e.getMessage());
        }
    }
}

