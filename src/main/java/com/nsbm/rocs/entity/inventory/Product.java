package com.nsbm.rocs.entity.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "InventoryProduct")
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "sku", unique = true, nullable = false, length = 60)
    private String sku;

    @Column(name = "barcode", unique = true, length = 60)
    private String barcode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "subcategory_id")
    private Long subcategoryId;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "unit_id")
    private Long unitId;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(name = "mrp", precision = 15, scale = 2)
    private BigDecimal mrp = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 15, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "max_stock_level", precision = 15, scale = 3)
    private BigDecimal maxStockLevel = BigDecimal.ZERO;

    @Column(name = "is_serialized")
    private Boolean isSerialized = false;


    @Column(name = "warranty_months")
    private Integer warrantyMonths = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

