package com.nsbm.rocs.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertDTO {
    private Long productId;
    private String productName;
    private String productSku;
    private Long branchId;
    private String branchName;
    private BigDecimal currentQuantity;
    private BigDecimal reorderLevel;
    private BigDecimal shortage;
    private String alertLevel; // LOW, CRITICAL, OUT_OF_STOCK
}


