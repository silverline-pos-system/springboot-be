package com.nsbm.rocs.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryAlertDTO {
    private Long batchId;
    private String batchCode;
    private Long productId;
    private String productName;
    private Long branchId;
    private String branchName;
    private LocalDate expiryDate;
    private Long daysUntilExpiry;
    private BigDecimal quantity;
    private String alertLevel; // EXPIRED, CRITICAL, WARNING, APPROACHING
    private String productSku;
    private BigDecimal qty;
    private Long daysToExpiry;
    private String message;
}


