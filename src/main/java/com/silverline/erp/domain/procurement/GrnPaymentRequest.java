package com.silverline.erp.domain.procurement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment request raised when a GRN is posted. Drives the supplier payment
 * workflow: supervisor approve, transfer to manager, process payment.
 */
@Entity
@Table(name = "grn_payment_requests")
@Getter
@Setter
@NoArgsConstructor
public class GrnPaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "grn_id", nullable = false)
    private Long grnId;

    @Column(name = "grn_no", length = 50)
    private String grnNo;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "invoice_no", length = 100)
    private String invoiceNo;

    /**
     * PENDING, SUPERVISOR_APPROVED, TRANSFERRED_TO_MANAGER, PROCESSING, PAID,
     * REJECTED.
     */
    @Column(length = 30)
    private String status = "PENDING";

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "supervisor_approved_by")
    private Long supervisorApprovedBy;

    @Column(name = "supervisor_approved_at")
    private LocalDateTime supervisorApprovedAt;

    @Column(name = "transferred_to_manager_by")
    private Long transferredToManagerBy;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
