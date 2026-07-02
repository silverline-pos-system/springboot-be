package com.silverline.erp.domain.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "item_dispatches", indexes = {
    @Index(name = "idx_dispatch_branch_status", columnList = "branch_id, status")
})
@Getter
@Setter
@NoArgsConstructor
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispatch_id")
    private Long dispatchId;

    @Column(name = "dispatch_no", unique = true, nullable = false, length = 50)
    private String dispatchNo;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "po_id")
    private Long poId;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "invoice_no", length = 100)
    private String invoiceNo;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus = "UNPAID";

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (dispatchDate == null) dispatchDate = LocalDate.now();
    }
}

