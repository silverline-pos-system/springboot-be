package com.silverline.erp.module.analytics.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.analytics.dto.BranchAlertDTO;
import com.silverline.erp.module.analytics.dto.ExpiryAlertDTO;
import com.silverline.erp.module.analytics.dto.StockAlertDTO;
import com.silverline.erp.module.analytics.service.AlertService;
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
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/inventory/alerts")
    public ResponseEntity<ApiResponse<List<StockAlertDTO>>> getStockAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching stock alerts for branchId: {}", branchId);
        List<StockAlertDTO> alerts = alertService.getStockAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stock alerts fetched successfully", alerts));
    }

    @GetMapping("/inventory/expiry-alerts")
    public ResponseEntity<ApiResponse<List<ExpiryAlertDTO>>> getExpiryAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching expiry alerts for branchId: {}", branchId);
        List<ExpiryAlertDTO> alerts = alertService.getExpiryAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Expiry alerts fetched successfully", alerts));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<BranchAlertDTO>>> getBranchAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching branch alerts for branchId: {}", branchId);
        List<BranchAlertDTO> alerts = alertService.getBranchAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Branch alerts fetched successfully", alerts));
    }
}
