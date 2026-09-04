package com.silverline.erp.domain.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-branch product price and carry list. A branch carries (and may sell) a
 * product only if a row exists here. Pricing is per branch: there is no global
 * product price.
 */
@Entity
@Table(name = "branch_product", uniqueConstraints = {
        @UniqueConstraint(name = "uq_branch_product", columnNames = {"branch_id", "product_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class BranchProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_product_id")
    private Long branchProductId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(name = "mrp", precision = 15, scale = 2)
    private BigDecimal mrp = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 15, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "added_by_branch_id")
    private Long addedByBranchId;

    @Column(name = "added_by_user_id")
    private Long addedByUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
