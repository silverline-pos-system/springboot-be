package com.silverline.erp.module.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReportDTO {
    private Long productId;
    private String productName;
    private String productSku;
    private Long branchId;
    private String branchName;
    private BigDecimal quantity;
    private BigDecimal reservedQty;
    private BigDecimal availableQty;
    private BigDecimal reorderLevel;
    private BigDecimal stockValue;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
}


