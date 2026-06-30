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
public class SupplierBranchDTO {
    private Long supplierId;
    private Long branchId;
    private String branchName;
    private Boolean isPreferred;
    private BigDecimal discountPercentage;
    private String notes;
}


