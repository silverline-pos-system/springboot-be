package com.silverline.erp.module.inventory.dto;

import com.silverline.erp.domain.inventory.Batch;
import jakarta.validation.constraints.NotBlank;
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
public class BatchDTO {
    private Long batchId;
    private Long productId;
    private String productName;
    private Long branchId;
    private String branchName;

    @NotBlank(message = "Batch code is required")
    private String batchCode;

    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private BigDecimal qty;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private String productSku;
    private Long daysToExpiry;
    private String expiryStatus;
}


