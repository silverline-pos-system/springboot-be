package com.silverline.erp.module.notification.service.impl;

import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.notification.Notification;
import com.silverline.erp.domain.notification.NotificationRecipient;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.infrastructure.sse.SseEmitterRegistry;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.notification.repository.NotificationRecipientRepository;
import com.silverline.erp.module.notification.repository.NotificationRepository;
import com.silverline.erp.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final UserRepository userRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Async
    @Override
    @Transactional
    public void createAndBroadcast(String type, String title, String message,
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

        List<UserProfile> recipients = userRepository.findByRoleInAndAccountStatus(
                List.of(Role.MANAGER, Role.SUPER_ADMIN, Role.SUPERVISOR),
                com.silverline.erp.domain.enums.AccountStatus.ACTIVE
        );

        for (UserProfile recipient : recipients) {
            NotificationRecipient nr = NotificationRecipient.builder()
                    .notification(notification)
                    .userId(recipient.getUserId())
                    .isRead(false)
                    .build();
            notificationRecipientRepository.save(nr);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", notification.getNotificationId());
        payload.put("type", notification.getType());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("referenceType", notification.getReferenceType());
        payload.put("referenceId", notification.getReferenceId());
        payload.put("priority", notification.getPriority());
        payload.put("createdAt", notification.getCreatedAt());

        for (UserProfile recipient : recipients) {
            sseEmitterRegistry.sendToUser(recipient.getUserId(), payload);
        }
        log.info("Broadcast SSE notification '{}' to {} managers/admins", title, recipients.size());
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

    @Async
    @Override
    public void broadcastDashboardUpdate(String eventType, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("data", data);
        payload.put("timestamp", LocalDateTime.now());

        sseEmitterRegistry.broadcast("dashboard-update", payload);
        log.info("Broadcast SSE dashboard update: {}", eventType);
    }
}
