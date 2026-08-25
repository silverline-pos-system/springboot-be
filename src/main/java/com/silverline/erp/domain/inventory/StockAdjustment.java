package com.silverline.erp.domain.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistent audit record of a manual stock adjustment. Replaces the previous in-memory list
 * that was lost on every restart and had no audit value.
 */
@Entity
@Table(name = "stock_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adjustment_id")
    private Long adjustmentId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "adjustment_type", nullable = false, length = 20)
    private String adjustmentType;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "quantity_before", precision = 15, scale = 3)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", precision = 15, scale = 3)
    private BigDecimal quantityAfter;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "reference_no", length = 50)
    private String referenceNo;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
