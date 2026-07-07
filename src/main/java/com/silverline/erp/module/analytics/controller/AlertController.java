package com.silverline.erp.module.analytics.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.analytics.dto.BranchAlertDTO;
import com.silverline.erp.module.analytics.dto.ExpiryAlertDTO;
import com.silverline.erp.module.analytics.dto.StockAlertDTO;
import com.silverline.erp.module.analytics.service.AlertService;
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
@Tag(name = "Stock & Expiry Alerts", description = "APIs for managers to check low-stock thresholds, upcoming batch expirations, and combined branch alert lists")
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "Get low stock alerts list", description = "Retrieves a list of stock alerts for products that are running low (with optional branch header filters)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock alerts list fetched successfully")
    @GetMapping("/inventory/alerts")
    public ResponseEntity<ApiResponse<List<StockAlertDTO>>> getStockAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching stock alerts for branchId: {}", branchId);
        List<StockAlertDTO> alerts = alertService.getStockAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stock alerts fetched successfully", alerts));
    }

    @Operation(summary = "Get upcoming batch expiry alerts", description = "Retrieves alerts for batches expiring in the near future (with optional branch header filters)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expiry alerts list fetched successfully")
    @GetMapping("/inventory/expiry-alerts")
    public ResponseEntity<ApiResponse<List<ExpiryAlertDTO>>> getExpiryAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching expiry alerts for branchId: {}", branchId);
        List<ExpiryAlertDTO> alerts = alertService.getExpiryAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Expiry alerts fetched successfully", alerts));
    }

    @Operation(summary = "Get combined branch alerts", description = "Retrieves all warnings (stock, expiry, approval delays) for a branch combined in a single list")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Combined branch alerts fetched successfully")
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<BranchAlertDTO>>> getBranchAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching branch alerts for branchId: {}", branchId);
        List<BranchAlertDTO> alerts = alertService.getBranchAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Branch alerts fetched successfully", alerts));
    }
}

