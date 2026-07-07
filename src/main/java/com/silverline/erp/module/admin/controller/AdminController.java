package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Metrics Summary", description = "APIs for loading global dashboard charts, KPIs, trends, and analytical overview widgets")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Get today's sales total", description = "Calculates total net sales amount across all branches for the current day")
    @ApiResponse(responseCode = "200", description = "Sales figure calculated successfully")
    @GetMapping("/today-sales")
    public ResponseEntity<BigDecimal> getTodaysSales() {
        return ResponseEntity.ok(adminService.getTodaysSales());
    }

    @Operation(summary = "Get user roles statistics", description = "Retrieves count of user accounts grouped by their primary roles (e.g. manager, cashier)")
    @ApiResponse(responseCode = "200", description = "User role statistics retrieved successfully")
    @GetMapping("/user-stats")
    public ResponseEntity<Map<String, Long>> getUserStatsByRole() {
        return ResponseEntity.ok(adminService.getUserStatsByRole());
    }

    @Operation(summary = "Get top performing branches", description = "Retrieves the top 5 branches by sales performance")
    @ApiResponse(responseCode = "200", description = "Top branches list retrieved successfully")
    @GetMapping("/top-branches")
    public ResponseEntity<List<Map<String, Object>>> getTopBranches() {
        return ResponseEntity.ok(adminService.getTopBranches(5));
    }

    @Operation(summary = "Get weekly sales trend", description = "Retrieves daily sales totals for the past 7 days to render trend lines")
    @ApiResponse(responseCode = "200", description = "Trend data compiled successfully")
    @GetMapping("/weekly-trend")
    public ResponseEntity<List<Map<String, Object>>> getWeeklySalesTrend() {
        return ResponseEntity.ok(adminService.getWeeklySalesTrend());
    }

    @Operation(summary = "Get general dashboard overview", description = "Fetches a combined map of dashboard KPI summaries for rapid initial dashboard loading")
    @ApiResponse(responseCode = "200", description = "Overview map compiled successfully")
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        return ResponseEntity.ok(adminService.getDashboardOverview());
    }

    @Operation(summary = "Get customer recurrence stats", description = "Analytical stub returning recurrence metrics")
    @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    @GetMapping("/customer-recurrence")
    public ResponseEntity<List<Map<String, Object>>> getCustomerRecurrence() {
        return ResponseEntity.ok(List.of());
    }

    @Operation(summary = "Get top branch managers list", description = "Analytical stub returning manager leaderboard metrics")
    @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    @GetMapping("/top-managers")
    public ResponseEntity<List<Map<String, Object>>> getTopManagers() {
        return ResponseEntity.ok(List.of());
    }
}
