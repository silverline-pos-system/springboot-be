package com.silverline.erp.module.analytics.controller;

import com.silverline.erp.module.analytics.dto.SalesReportDTO;
import com.silverline.erp.module.finance.service.AccountingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/analytics/reports", "/api/inventory/reports"})
@RequiredArgsConstructor
@Tag(name = "Printable Reports Exports", description = "APIs for querying and exporting printable reports (sales report listings, etc.)")
public class ReportsController {

    private final AccountingService accountingService;

    // ===== SALES REPORTS =====

    @Operation(summary = "Get sales reports list", description = "Retrieves sales totals and invoices between the specified 'from' and 'to' date parameters")
    @ApiResponse(responseCode = "200", description = "Reports list retrieved successfully")
    @GetMapping("/sales")
    public ResponseEntity<List<SalesReportDTO>> getSalesReports(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("Fetching sales reports from: {} to: {}", from, to);
        List<SalesReportDTO> reports = accountingService.getSalesReports(from, to);
        return ResponseEntity.ok(reports);
    }
}
