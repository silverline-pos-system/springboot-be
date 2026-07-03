package com.silverline.erp.module.analytics.controller;

import com.silverline.erp.module.analytics.dto.SalesReportDTO;
import com.silverline.erp.module.finance.service.AccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Reports endpoints.
 * Base path: /api/inventory/reports
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/analytics/reports", "/api/inventory/reports"})
@RequiredArgsConstructor
public class ReportsController {

    private final AccountingService accountingService;

    // ===== SALES REPORTS =====

    @GetMapping("/sales")
    public ResponseEntity<List<SalesReportDTO>> getSalesReports(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("Fetching sales reports from: {} to: {}", from, to);
        List<SalesReportDTO> reports = accountingService.getSalesReports(from, to);
        return ResponseEntity.ok(reports);
    }
}


