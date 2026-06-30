package com.nsbm.rocs.modules.manager.controller;

import com.nsbm.rocs.modules.manager.dto.*;
import com.nsbm.rocs.modules.manager.service.ManagerService;
import com.nsbm.rocs.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Manager Dashboard endpoints.
 * Base path: /api/v1/manager
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;
    private final com.nsbm.rocs.modules.manager.service.JasperReportService jasperReportService;

    // ===== DASHBOARD STATS =====

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<DashboardStatsDTO>>> getDashboardStats(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching dashboard stats for branchId: {}", branchId);
        List<DashboardStatsDTO> stats = managerService.getDashboardStats(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stats fetched successfully", stats));
    }

    // ===== SALES DATA =====

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<List<SalesDataDTO>>> getSalesData(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching sales data for period: {}, branchId: {}", period, branchId);
        List<SalesDataDTO> salesData = managerService.getSalesData(period, branchId);
        return ResponseEntity.ok(ApiResponse.success("Sales data fetched successfully", salesData));
    }

    // ===== COMPREHENSIVE SALES ANALYTICS =====

    @GetMapping("/sales/analytics")
    public ResponseEntity<ApiResponse<SalesAnalyticsDTO>> getSalesAnalytics(
            @RequestParam(defaultValue = "daily") String period,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching comprehensive sales analytics for period: {}, branchId: {}", period, branchId);
        SalesAnalyticsDTO analytics = managerService.getSalesAnalytics(period, branchId);
        return ResponseEntity.ok(ApiResponse.success("Analytics fetched successfully", analytics));
    }

    // ===== SALES REPORTS =====

    @GetMapping("/reports/sales")
    public ResponseEntity<ApiResponse<List<SalesReportDTO>>> getSalesReports(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching sales reports from {} to {} for branchId: {}", startDate, endDate, branchId);
        List<SalesReportDTO> reports = managerService.getSalesReports(startDate, endDate, branchId);
        return ResponseEntity.ok(ApiResponse.success("Reports fetched successfully", reports));
    }

    @GetMapping("/reports/sales/summary-by-terminal")
    public ResponseEntity<ApiResponse<List<TerminalSalesDTO>>> getSalesSummaryByTerminal(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching sales summary by terminal from {} to {} for branchId: {}", startDate, endDate, branchId);
        List<TerminalSalesDTO> reports = managerService.getSalesByTerminal(startDate, endDate, branchId);
        return ResponseEntity.ok(ApiResponse.success("Terminal summary fetched successfully", reports));
    }

    // ===== TOP SELLING PRODUCTS =====

    @GetMapping("/products/top-selling")
    public ResponseEntity<ApiResponse<List<TopSellingProductDTO>>> getTopSellingProducts(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Fetching top {} selling products", limit);
        List<TopSellingProductDTO> products = managerService.getTopSellingProducts(limit, null);
        return ResponseEntity.ok(ApiResponse.success("Top products fetched successfully", products));
    }

    // ===== PENDING DISPATCHES =====

    @GetMapping("/dispatches/pending")
    public ResponseEntity<ApiResponse<List<PendingDispatchDTO>>> getPendingDispatches(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching pending dispatches for branchId: {}", branchId);
        List<PendingDispatchDTO> dispatches = managerService.getPendingDispatches(branchId);
        return ResponseEntity.ok(ApiResponse.success("Pending dispatches fetched successfully", dispatches));
    }

    // ===== STAFF SUMMARY =====

    @GetMapping("/staff/summary")
    public ResponseEntity<ApiResponse<List<StaffSummaryDTO>>> getStaffSummary(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching staff summary for branchId: {}", branchId);
        List<StaffSummaryDTO> staff = managerService.getStaffSummary(branchId);
        return ResponseEntity.ok(ApiResponse.success("Staff summary fetched successfully", staff));
    }

    // ===== STOCK ALERTS =====

    @GetMapping("/inventory/alerts")
    public ResponseEntity<ApiResponse<List<StockAlertDTO>>> getStockAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching stock alerts for branchId: {}", branchId);
        List<StockAlertDTO> alerts = managerService.getStockAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stock alerts fetched successfully", alerts));
    }

    // ===== EXPIRY ALERTS =====

    @GetMapping("/inventory/expiry-alerts")
    public ResponseEntity<ApiResponse<List<ExpiryAlertDTO>>> getExpiryAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching expiry alerts for branchId: {}", branchId);
        List<ExpiryAlertDTO> alerts = managerService.getExpiryAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Expiry alerts fetched successfully", alerts));
    }

    // ===== BRANCH ALERTS =====

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<BranchAlertDTO>>> getBranchAlerts(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching branch alerts for branchId: {}", branchId);
        List<BranchAlertDTO> alerts = managerService.getBranchAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Branch alerts fetched successfully", alerts));
    }

    // ===== APPROVALS =====

    @GetMapping("/approvals")
    public ResponseEntity<ApiResponse<List<ApprovalDTO>>> getApprovals(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching approvals with status: {}, branchId: {}", status, branchId);
        List<ApprovalDTO> approvals = managerService.getApprovals(status, branchId);
        return ResponseEntity.ok(ApiResponse.success("Approvals fetched successfully", approvals));
    }

    @GetMapping("/approvals/me")
    public ResponseEntity<ApiResponse<List<ApprovalDTO>>> getMyApprovals(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching my approval requests for branchId: {}", branchId);
        List<ApprovalDTO> approvals = managerService.getMyApprovals(branchId);
        return ResponseEntity.ok(ApiResponse.success("Your approvals fetched successfully", approvals));
    }

    @PatchMapping("/approvals/{approvalId}")
    public ResponseEntity<ApiResponse<ApprovalDTO>> updateApprovalStatus(
            @PathVariable Long approvalId,
            @RequestBody ApprovalUpdateRequest request) {
        log.info("Updating approval {} to status: {}", approvalId, request.getStatus());
        ApprovalDTO approval = managerService.updateApprovalStatus(
                approvalId,
                request.getStatus(),
                request.getNotes(),
                request.getRole(),
                request.getApproverId()
        );
        return ResponseEntity.ok(ApiResponse.success("Approval status updated", approval));
    }

    // ===== BRANCH ACTIVITY LOG =====

    @GetMapping("/activity-log")
    public ResponseEntity<ApiResponse<List<ActivityLogDTO>>> getBranchActivityLog(
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching branch activity log with limit: {}, branchId: {}", limit, branchId);
        List<ActivityLogDTO> activities = managerService.getBranchActivityLog(limit, branchId);
        return ResponseEntity.ok(ApiResponse.success("Activity log fetched successfully", activities));
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
