package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalyticsDTO {
    // Summary Stats
    private BigDecimal todaySales;
    private BigDecimal yesterdaySales;
    private BigDecimal weeklyAverage;
    private int todayTransactions;
    private int yesterdayTransactions;
    private BigDecimal avgTransactionValue;
    private int customersServed;
    private double growthPercentage;
    
    // Breakdown Data
    private List<PaymentBreakdownDTO> paymentBreakdown;
    private List<HourlySalesDTO> hourlySales;
    private List<RecentTransactionDTO> recentTransactions;
    private List<TopSellingProductDTO> topProducts;
    private List<SalesDataDTO> dailyTrend;
}

