package com.silverline.erp.module.notification.repository;

import com.silverline.erp.domain.notification.NotificationRecipient;
import com.silverline.erp.domain.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n JOIN NotificationRecipient nr ON nr.notification.notificationId = n.notificationId " +
           "WHERE nr.userId = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n JOIN NotificationRecipient nr ON nr.notification.notificationId = n.notificationId " +
           "WHERE nr.userId = :userId AND nr.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipientUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(nr) FROM NotificationRecipient nr WHERE nr.userId = :userId AND nr.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    List<Notification> findByTypeAndReferenceIdOrderByCreatedAtDesc(String type, Long referenceId);

    Page<Notification> findByTypeOrderByCreatedAtDesc(String type, Pageable pageable);

    List<Notification> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);
}
