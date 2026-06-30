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
public class SalesReportDTO {
    private String date;
    private String dayName;
    private int invoices;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal profit;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal qrSales;
    private BigDecimal returns;
    private BigDecimal avgBasket;
    private double profitMargin;
}

