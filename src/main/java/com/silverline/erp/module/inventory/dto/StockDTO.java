package com.silverline.erp.module.inventory.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDTO {
    private Long stockId;
    private Long branchId;
    private String branchName;
    private Long productId;
    private String productName;
    private String productSku;

    @Min(value = 0, message = "Quantity must be positive")
    private BigDecimal quantity;

    @Min(value = 0, message = "Reserved quantity must be positive")
    private BigDecimal reservedQty;

    private BigDecimal availableQty;
    private BigDecimal reorderLevel;
    private Boolean isLowStock;
    private LocalDateTime lastUpdated;
}


