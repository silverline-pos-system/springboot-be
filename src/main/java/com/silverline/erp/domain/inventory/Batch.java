package com.silverline.erp.domain.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "InventoryBatch")
@Table(name = "batches", indexes = {
    @Index(name = "idx_batch_product_expiry", columnList = "product_id, expiry_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "batch_code", nullable = false, length = 60)
    private String batchCode;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "qty", precision = 15, scale = 3)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "mrp", precision = 15, scale = 2)
    private BigDecimal mrp;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

