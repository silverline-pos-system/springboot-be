package com.silverline.erp.domain.repair;

import com.silverline.erp.domain.enums.RepairStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "repair_jobs")
@Getter
@Setter
@NoArgsConstructor
public class RepairJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repair_id")
    private Long repairId;

    @Column(name = "repair_no", unique = true, nullable = false, length = 50)
    private String repairNo;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "device_brand", length = 100)
    private String deviceBrand;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "imei_no", length = 100)
    private String imeiNo;

    @Column(name = "problem_description", columnDefinition = "TEXT")
    private String problemDescription;

    @Column(name = "diagnosis_notes", columnDefinition = "TEXT")
    private String diagnosisNotes;

    @Column(name = "cost_note", columnDefinition = "TEXT")
    private String costNote;

    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "final_cost", precision = 15, scale = 2)
    private BigDecimal finalCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private RepairStatus status = RepairStatus.RECEIVED;

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
