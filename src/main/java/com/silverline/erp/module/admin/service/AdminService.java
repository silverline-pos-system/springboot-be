package com.silverline.erp.module.admin.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Admin Dashboard operations
 */
public interface AdminService {

    /**
     * Get today's total sales amount
     * @return Total sales for today
     */
    BigDecimal getTodaysSales();

    /**
     * Get user statistics grouped by role
     * @return Map of role to user count
     */
    Map<String, Long> getUserStatsByRole();

    /**
     * Get top performing branches by sales
     * @param limit Number of branches to return
     * @return List of branch data with sales info
     */
    List<Map<String, Object>> getTopBranches(int limit);

    /**
     * Get weekly sales trend data
     * @return List of daily sales data for the past week
     */
    List<Map<String, Object>> getWeeklySalesTrend();

    /**
     * Get complete dashboard overview
     * @return Map containing various dashboard metrics
     */
    Map<String, Object> getDashboardOverview();
}

