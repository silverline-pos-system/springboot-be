package com.silverline.erp.domain.procurement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GRN (Goods Received Note) header. Records a supplier delivery received into a
 * single branch against a Purchase Order. Replaces the old Item Dispatch.
 */
@Entity
@Table(name = "grn", indexes = {
        @Index(name = "idx_grn_branch_status", columnList = "branch_id, status")
})
@Getter
@Setter
@NoArgsConstructor
public class Grn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grn_id")
    private Long grnId;

    @Column(name = "grn_no", unique = true, nullable = false, length = 50)
    private String grnNo;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "po_id")
    private Long poId;

    @Column(name = "grn_date", nullable = false)
    private LocalDate grnDate;

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

    /** DRAFT then POSTED (stock updated) or CANCELLED. */
    @Column(length = 20)
    private String status = "DRAFT";

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "posted_by")
    private Long postedBy;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (grnDate == null) grnDate = LocalDate.now();
    }
}
