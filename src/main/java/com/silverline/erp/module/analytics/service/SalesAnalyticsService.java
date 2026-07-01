package com.silverline.erp.module.analytics.service;

import com.silverline.erp.module.analytics.dto.*;

import java.util.List;

public interface SalesAnalyticsService {
    List<SalesDataDTO> getSalesData(String period, Long branchId);
    List<TopSellingProductDTO> getTopSellingProducts(int limit, Long branchId);
    SalesAnalyticsDTO getSalesAnalytics(String period, Long branchId);
    List<SalesReportDTO> getSalesReports(String startDateStr, String endDateStr, Long branchId);
    List<TerminalSalesDTO> getSalesByTerminal(String startDateStr, String endDateStr, Long branchId);
}
