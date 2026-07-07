package com.silverline.erp.module.analytics.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.analytics.dto.DashboardStatsDTO;
import com.silverline.erp.module.analytics.service.DashboardService;
import com.silverline.erp.module.manager.dto.PendingDispatchDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
@Tag(name = "Dashboard Metrics Hub", description = "APIs for managers to fetch overview charts, daily counts, and alert notifications dashboard statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get manager dashboard statistics", description = "Retrieves overview KPI cards (total sales, stock value, shifts count) with optional branch filter headers")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stats fetched successfully")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<DashboardStatsDTO>>> getDashboardStats(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching dashboard stats for branchId: {}", branchId);
        List<DashboardStatsDTO> stats = dashboardService.getDashboardStats(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stats fetched successfully", stats));
    }

    @Operation(summary = "Get pending dispatches overview", description = "Lists active dispatches awaiting manager receipt/approval (with optional branch filter headers)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending dispatches fetched successfully")
    @GetMapping("/dispatches/pending")
    public ResponseEntity<ApiResponse<List<PendingDispatchDTO>>> getPendingDispatches(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching pending dispatches for branchId: {}", branchId);
        List<PendingDispatchDTO> dispatches = dashboardService.getPendingDispatches(branchId);
        return ResponseEntity.ok(ApiResponse.success("Pending dispatches fetched successfully", dispatches));
    }
}

