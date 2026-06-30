package com.nsbm.rocs.entity.main;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // EMPLOYEE_REGISTERED, SHIFT_REQUESTED, RETURN_REQUESTED, PO_PAYMENT_REQUEST, PASSWORD_RESET_REQUEST

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_type", length = 50)
    private String referenceType; // USER, CASH_SHIFT, SALES_RETURN, PURCHASE_ORDER, PASSWORD_RESET

    @Column(name = "reference_id")
    private Long referenceId;

    @Builder.Default
    @Column(name = "priority", length = 20)
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (priority == null) {
            priority = "NORMAL";
        }
    }
}
