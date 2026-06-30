package com.nsbm.rocs.entity.services;

import com.nsbm.rocs.entity.enums.RepairStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "repair_status_history")
@Getter
@Setter
@NoArgsConstructor
public class RepairStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "repair_id", nullable = false)
    private Long repairId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private RepairStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private RepairStatus newStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
}
