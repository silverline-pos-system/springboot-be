package com.silverline.erp.module.notification.service;

import com.silverline.erp.domain.notification.Notification;
import com.silverline.erp.domain.notification.NotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface NotificationService {
    void createAndBroadcast(String type, String title, String message,
                            String referenceType, Long referenceId,
                            String priority, Long createdBy);
    Page<NotificationRecipient> getNotificationsForUser(Long userId, Pageable pageable);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
    void broadcastDashboardUpdate(String eventType, Map<String, Object> data);
}
