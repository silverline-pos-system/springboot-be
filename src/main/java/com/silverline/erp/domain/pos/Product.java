package com.silverline.erp.domain.pos;

import com.silverline.erp.domain.inventory.Stock;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false, unique = true, length = 60)
    private String sku;

    @Column(unique = true, length = 60)
    private String barcode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /* -------------------- Relationships -------------------- */

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "subcategory_id")
    private SubCategory subCategory;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne
    @JoinColumn(name = "unit_id")
    private Unit unit;

    /* -------------------- Pricing -------------------- */

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal mrp = BigDecimal.ZERO;

    /* -------------------- Stock Management -------------------- */

    @Column(name = "reorder_level", precision = 15, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "max_stock_level", precision = 15, scale = 3)
    private BigDecimal maxStockLevel = BigDecimal.ZERO;

    @Column(name = "is_serialized")
    private Boolean isSerialized = false;

    /* -------------------- Tax & Warranty -------------------- */


    @Column(name = "warranty_months")
    private Integer warrantyMonths = 0;

    /* -------------------- Status & Audit -------------------- */

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* -------------------- Lifecycle Hooks -------------------- */

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /* -------------------- Getters & Setters -------------------- */

    // Generate getters and setters using IDE (recommended)
}
