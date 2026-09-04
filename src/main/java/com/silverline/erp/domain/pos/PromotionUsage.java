package com.silverline.erp.domain.pos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Audit of a promotion applied to a sale, with the benefit given. */
@Entity
@Table(name = "promotion_usage")
@Getter
@Setter
@NoArgsConstructor
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long usageId;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "benefit_amount", precision = 15, scale = 2)
    private BigDecimal benefitAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
