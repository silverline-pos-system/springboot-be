package com.silverline.erp.module.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long productId;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String barcode;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private Long categoryId;
    private String categoryName;

    private Long subcategoryId;
    private String subcategoryName;

    private Long brandId;
    private String brandName;

    private Long unitId;
    private String unitName;
    private String unitSymbol;

    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;

    private BigDecimal reorderLevel;
    private BigDecimal maxStockLevel;

    private Boolean isSerialized;
    private Integer warrantyMonths;
    private Boolean isActive;

    // Stock quantity (aggregated from stock table)
    private BigDecimal quantity;
}


