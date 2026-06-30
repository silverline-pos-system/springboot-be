package com.silverline.erp.module.procurement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispatchStatsDTO {

    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalDispatches;
    private Long pendingDispatches;
    private Long approvedDispatches;
    private Long rejectedDispatches;
    private BigDecimal totalValue;
    private BigDecimal unpaidAmount;
    private BigDecimal paidAmount;
    private Long totalItems;
    private Long uniqueProducts;
    private Long activeSuppliers;
}

