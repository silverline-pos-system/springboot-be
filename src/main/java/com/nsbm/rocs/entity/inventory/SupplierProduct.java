package com.nsbm.rocs.entity.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_products")
@Getter
@Setter
@NoArgsConstructor
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_product_id")
    private Long supplierProductId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "supplier_sku", length = 100)
    private String supplierSku;

    @Column(name = "cost_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal costPrice;

    @Column(name = "moq")
    private Integer moq = 1;

    @Column(name = "is_preferred")
    private Boolean isPreferred = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

