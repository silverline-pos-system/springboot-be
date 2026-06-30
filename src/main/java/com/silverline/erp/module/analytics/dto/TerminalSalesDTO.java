package com.silverline.erp.module.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalSalesDTO {
    private Long id;
    private String name;
    private String code;
    private BigDecimal revenue;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private Integer transactionCount;
    private BigDecimal totalRevenue; // For percentage calculation if needed
}

