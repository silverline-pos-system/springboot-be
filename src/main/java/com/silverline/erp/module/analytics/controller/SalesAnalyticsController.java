package com.silverline.erp.module.analytics.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.infrastructure.reporting.JasperReportService;
import com.silverline.erp.module.analytics.dto.*;
import com.silverline.erp.module.analytics.service.SalesAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class SalesAnalyticsController {

    private final SalesAnalyticsService salesAnalyticsService;
    private final JasperReportService jasperReportService;

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<List<SalesDataDTO>>> getSalesData(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching sales data for period: {}, branchId: {}", period, branchId);
        List<SalesDataDTO> salesData = salesAnalyticsService.getSalesData(period, branchId);
        return ResponseEntity.ok(ApiResponse.success("Sales data fetched successfully", salesData));
    }

    @GetMapping("/sales/analytics")
    public ResponseEntity<ApiResponse<SalesAnalyticsDTO>> getSalesAnalytics(
            @RequestParam(defaultValue = "daily") String period,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching comprehensive sales analytics for period: {}, branchId: {}", period, branchId);
        SalesAnalyticsDTO analytics = salesAnalyticsService.getSalesAnalytics(period, branchId);
        return ResponseEntity.ok(ApiResponse.success("Analytics fetched successfully", analytics));
    }

    @GetMapping("/reports/sales")
    public ResponseEntity<ApiResponse<List<SalesReportDTO>>> getSalesReports(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching sales reports from {} to {} for branchId: {}", startDate, endDate, branchId);
        List<SalesReportDTO> reports = salesAnalyticsService.getSalesReports(startDate, endDate, branchId);
        return ResponseEntity.ok(ApiResponse.success("Reports fetched successfully", reports));
    }

    @GetMapping("/reports/sales/summary-by-terminal")
    public ResponseEntity<ApiResponse<List<TerminalSalesDTO>>> getSalesSummaryByTerminal(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching sales summary by terminal from {} to {} for branchId: {}", startDate, endDate, branchId);
        List<TerminalSalesDTO> reports = salesAnalyticsService.getSalesByTerminal(startDate, endDate, branchId);
        return ResponseEntity.ok(ApiResponse.success("Terminal summary fetched successfully", reports));
    }

    @GetMapping("/products/top-selling")
    public ResponseEntity<ApiResponse<List<TopSellingProductDTO>>> getTopSellingProducts(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Fetching top {} selling products", limit);
        List<TopSellingProductDTO> products = salesAnalyticsService.getTopSellingProducts(limit, null);
        return ResponseEntity.ok(ApiResponse.success("Top products fetched successfully", products));
    }

    // ===== PDF REPORTS =====

    @GetMapping("/reports/sales/pdf")
    public ResponseEntity<byte[]> getSalesReportsPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        try {
            byte[] pdfBytes = jasperReportService.generateSalesReportsPdf(startDate, endDate, branchId);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_report.pdf")
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generating sales report PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/activity-log/pdf")
    public ResponseEntity<byte[]> getBranchActivityLogPdf(
            @RequestParam(defaultValue = "100") int limit,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        try {
            byte[] pdfBytes = jasperReportService.generateBranchActivityLogPdf(limit, branchId);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=activity_log.pdf")
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generating activity log PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/loyalty/pdf")
    public ResponseEntity<byte[]> getLoyaltyCustomersPdf() {
        try {
            byte[] pdfBytes = jasperReportService.generateLoyaltyCustomersPdf();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loyalty_customers.pdf")
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generating loyalty customers PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/dispatches/pdf")
    public ResponseEntity<byte[]> getDispatchListPdf() {
        try {
            byte[] pdfBytes = jasperReportService.generateDispatchListPdf();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dispatch_list.pdf")
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generating dispatch list PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reports/approvals/pdf")
    public ResponseEntity<byte[]> getApprovalHistoryPdf(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        try {
            byte[] pdfBytes = jasperReportService.generateApprovalHistoryPdf(branchId);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=approval_history.pdf")
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generating approval history PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
