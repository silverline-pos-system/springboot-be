package com.silverline.erp.domain.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dispatch Payment Request entity - tracks payment requests for approved Dispatches
 * that need to be processed by the finance/accounting department
 */
@Entity
@Table(name = "dispatch_payment_requests")
@Getter
@Setter
@NoArgsConstructor
public class DispatchPaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "dispatch_id", nullable = false)
    private Long dispatchId;

    @Column(name = "dispatch_no", length = 50)
    private String dispatchNo;

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
     * Status of the payment request:
     * - PENDING: Awaiting processing
     * - SUPERVISOR_APPROVED: Approved by supervisor, ready for manager
     * - TRANSFERRED_TO_MANAGER: Sent to manager for payment processing
     * - PROCESSING: Manager is processing the payment
     * - PAID: Payment has been made
     * - REJECTED: Request was rejected
     */
    @Column(length = 30)
    private String status = "PENDING";

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL"; // URGENT, HIGH, NORMAL, LOW

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
    private String paymentMethod; // BANK_TRANSFER, CHEQUE, CASH

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
