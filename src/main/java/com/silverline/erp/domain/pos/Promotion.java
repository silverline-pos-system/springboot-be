package com.silverline.erp.domain.pos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A promotion/offer campaign. Branch-scoped (null branch = all branches),
 * time-bounded, and applied at POS checkout. Multiple active promotions may stack.
 */
@Entity
@Table(name = "promotions", indexes = {
        @Index(name = "idx_promotions_branch_active", columnList = "branch_id, is_active")
})
@Getter
@Setter
@NoArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", nullable = false, length = 30)
    private PromoType promoType;

    /** null = applies to all branches. */
    @Column(name = "branch_id")
    private Long branchId;

    /** PRODUCT, CATEGORY or ALL. */
    @Column(name = "scope_type", nullable = false, length = 20)
    private String scopeType;

    /** product_id or category_id depending on scopeType; null for ALL. */
    @Column(name = "scope_ref_id")
    private Long scopeRefId;

    @Column(name = "buy_qty", precision = 15, scale = 3)
    private BigDecimal buyQty;

    @Column(name = "get_qty", precision = 15, scale = 3)
    private BigDecimal getQty;

    /** null = the free item is the same product bought. */
    @Column(name = "get_product_id")
    private Long getProductId;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "fixed_price", precision = 15, scale = 2)
    private BigDecimal fixedPrice;

    @Column(name = "min_cart_amount", precision = 15, scale = 2)
    private BigDecimal minCartAmount;

    /** EXPIRY_CLEARANCE: qualify batches within this many days of expiry. */
    @Column(name = "clearance_days")
    private Integer clearanceDays;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(nullable = false)
    private Boolean stackable = true;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** null = unlimited. */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "uses_count", nullable = false)
    private Integer usesCount = 0;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
