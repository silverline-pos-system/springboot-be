package com.silverline.erp.module.notification.repository;

import com.silverline.erp.domain.notification.NotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    Optional<NotificationRecipient> findByNotification_NotificationIdAndUserId(Long notificationId, Long userId);

    List<NotificationRecipient> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    Page<NotificationRecipient> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationRecipient nr SET nr.isRead = true, nr.readAt = :readAt WHERE nr.userId = :userId AND nr.isRead = false")
    int markAllAsReadForUser(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationRecipient nr SET nr.isRead = true, nr.readAt = :readAt " +
           "WHERE nr.notification.notificationId = :notificationId AND nr.userId = :userId")
    int markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    List<NotificationRecipient> findByNotification_NotificationId(Long notificationId);
}
