package com.nsbm.rocs.entity.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_performance")
@Getter
@Setter
@NoArgsConstructor
public class SupplierPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "performance_id")
    private Long performanceId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "quality_rating")
    private Integer qualityRating;

    @Column(name = "delivery_rating")
    private Integer deliveryRating;

    @Column(name = "price_rating")
    private Integer priceRating;

    @Column(name = "overall_rating", precision = 3, scale = 2)
    private BigDecimal overallRating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "evaluated_by")
    private Long evaluatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (evaluationDate == null) evaluationDate = LocalDate.now();
    }
}

