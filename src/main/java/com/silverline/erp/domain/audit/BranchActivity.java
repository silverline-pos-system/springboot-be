package com.silverline.erp.domain.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;


@Entity
@Table(name = "branch_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    // NOTE: terminal_id REMOVED â€” terminal concept eliminated

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action_type", nullable = false)
    private String actionType; // LOGIN, SALE, RETURN, etc.

    @Column(name = "entity_type")
    private String entityType; // SALE, PRODUCT, STOCK

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", length = 500)
    private String details;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Builder.Default
    @Column(length = 20)
    private String severity = "INFO"; // INFO, WARNING, CRITICAL

    @Builder.Default
    @Column(length = 20)
    private String status = "SUCCESS"; // SUCCESS, FAILED

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Transient fields for service layer convenience
    @Transient
    private String username;
    @Transient
    private String userRole;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (severity == null) severity = "INFO";
        if (status == null) status = "SUCCESS";
    }
}
