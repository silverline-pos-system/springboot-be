package com.silverline.erp.module.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ExpenseDashboardDTO {
    private BigDecimal todayExpenses;
    private BigDecimal monthlyExpenses;
    private BigDecimal unpaidExpenses;
    private String topCategoryName;
    private BigDecimal topCategoryAmount;

    private List<Map<String, Object>> monthlyCategoryBreakdown;
    private List<Map<String, Object>> branchExpenseBreakdown;
}

