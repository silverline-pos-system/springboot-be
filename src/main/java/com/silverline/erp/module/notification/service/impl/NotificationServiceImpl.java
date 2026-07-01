package com.silverline.erp.module.notification.service.impl;

import com.silverline.erp.domain.notification.Notification;
import com.silverline.erp.domain.notification.NotificationRecipient;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.module.notification.repository.NotificationRepository;
import com.silverline.erp.module.notification.repository.NotificationRecipientRepository;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.notification.service.NotificationService;
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
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Notification createAndBroadcast(String type, String title, String message,
                                            String referenceType, Long referenceId,
                                            String priority, Long createdBy) {
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

        List<UserProfile> recipients = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null &&
                        (u.getRole() == Role.MANAGER ||
                         u.getRole() == Role.SUPER_ADMIN))
                .filter(u -> u.getAccountStatus() != null &&
                        u.getAccountStatus().name().equals("ACTIVE"))
                .collect(Collectors.toList());

        for (UserProfile recipient : recipients) {
            NotificationRecipient nr = NotificationRecipient.builder()
                    .notification(notification)
                    .userId(recipient.getUserId())
                    .isRead(false)
                    .build();
            notificationRecipientRepository.save(nr);
        }

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

    @Override
    public Page<NotificationRecipient> getNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRecipientRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRecipientRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRecipientRepository.markAsRead(notificationId, userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRecipientRepository.markAllAsReadForUser(userId, LocalDateTime.now());
    }

    @Override
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
