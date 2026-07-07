package com.silverline.erp.module.procurement.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.infrastructure.reporting.JasperReportService;
import com.silverline.erp.module.procurement.dto.*;
import com.silverline.erp.module.procurement.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("inventoryDispatchController")
@RequestMapping({"/api/v1/procurement/dispatch", "/api/inventory/dispatch"})
@RequiredArgsConstructor
@Validated
@Tag(name = "Supplier Dispatches", description = "APIs for tracking incoming supplier dispatches, approving shipments, logging payments, and exporting PDF manifests")
public class DispatchController {

    private final DispatchService dispatchService;
    private final JasperReportService jasperReportService;

    @Operation(summary = "Create supplier dispatch record", description = "Logs a new incoming stock dispatch from a supplier vendor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Dispatch logged successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or schema validation failure")
    @PostMapping
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> createDispatch(
            @Valid @RequestBody DispatchCreateRequestDTO request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Creating Dispatch, User: {}", currentUserId);
        DispatchResponseDTO result = dispatchService.createDispatch(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dispatch created successfully", result));
    }

    @Operation(summary = "Get dispatch by ID", description = "Retrieves shipment details, supplier reference, and item lists by dispatch database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatch retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch record not found")
    @GetMapping("/{dispatchId}")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> getDispatchById(
            @PathVariable @NotNull Long dispatchId) {
        log.info("Fetching Dispatch ID: {}", dispatchId);
        DispatchResponseDTO result = dispatchService.getDispatchById(dispatchId);
        return ResponseEntity.ok(ApiResponse.success("Dispatch retrieved successfully", result));
    }

    @Operation(summary = "Get dispatches by branch", description = "Lists dispatches destined for a specific branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatches list retrieved successfully")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<DispatchResponseDTO>>> getDispatchesByBranch(
            @PathVariable @NotNull Long branchId) {
        log.info("Fetching Dispatches for branch: {}", branchId);
        List<DispatchResponseDTO> result = dispatchService.getDispatchesByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Dispatches retrieved successfully", result));
    }

    @Operation(summary = "Search dispatches by filter", description = "Queries dispatches matching filter criteria (status, supplier, date range)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatches search completed successfully")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<DispatchResponseDTO>>> searchDispatches(
            @RequestBody DispatchFilterDTO filter) {
        log.info("Searching Dispatches");
        List<DispatchResponseDTO> result = dispatchService.searchDispatches(filter);
        return ResponseEntity.ok(ApiResponse.success("Dispatches retrieved successfully", result));
    }

    @Operation(summary = "Update dispatch details", description = "Modifies draft dispatch items, transport info, or invoices")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatch updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch record not found")
    @PutMapping("/{dispatchId}")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> updateDispatch(
            @PathVariable @NotNull Long dispatchId,
            @Valid @RequestBody DispatchUpdateRequestDTO request) {
        log.info("Updating Dispatch ID: {}", dispatchId);
        DispatchResponseDTO result = dispatchService.updateDispatch(dispatchId, request);
        return ResponseEntity.ok(ApiResponse.success("Dispatch updated successfully", result));
    }

    @Operation(summary = "Approve incoming dispatch", description = "Approves a pending dispatch, increasing available item stocks at the target branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatch approved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid dispatch state or approval error")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch record not found")
    @PutMapping("/{dispatchId}/approve")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> approveDispatch(
            @PathVariable @NotNull Long dispatchId) {
        Long approvedBy = SecurityUtils.getCurrentUserId();
        log.info("Approving Dispatch ID: {}, Approved By: {}", dispatchId, approvedBy);
        DispatchResponseDTO result = dispatchService.approveDispatch(dispatchId, approvedBy);
        return ResponseEntity.ok(ApiResponse.success("Dispatch approved successfully", result));
    }

    @Operation(summary = "Update dispatch payment status", description = "Modifies payment settlements status for a dispatch invoice (e.g. PAID, UNPAID)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment status updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch record not found")
    @PutMapping("/{dispatchId}/payment-status")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> updatePaymentStatus(
            @PathVariable @NotNull Long dispatchId,
            @RequestParam String paymentStatus) {
        log.info("Updating payment status for Dispatch ID: {} to {}", dispatchId, paymentStatus);
        DispatchResponseDTO result = dispatchService.updatePaymentStatus(dispatchId, paymentStatus);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", result));
    }

    @Operation(summary = "Delete dispatch entry", description = "Deletes a draft dispatch record")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatch deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot delete a dispatch that has been approved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch record not found")
    @DeleteMapping("/{dispatchId}")
    public ResponseEntity<ApiResponse<Void>> deleteDispatch(
            @PathVariable @NotNull Long dispatchId) {
        log.info("Deleting Dispatch ID: {}", dispatchId);
        dispatchService.deleteDispatch(dispatchId);
        return ResponseEntity.ok(ApiResponse.success("Dispatch deleted successfully"));
    }

    @Operation(summary = "Get branch dispatch statistics", description = "Retrieves purchase statistics, received counts, and financial summaries for a branch and period")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stats compiled successfully")
    @GetMapping("/branch/{branchId}/stats")
    public ResponseEntity<ApiResponse<DispatchStatsDTO>> getDispatchStats(
            @PathVariable @NotNull Long branchId,
            @RequestParam(required = false) String period) {
        log.info("Fetching Dispatch stats for branch: {}, period: {}", branchId, period);
        DispatchStatsDTO result = dispatchService.getDispatchStats(branchId, period);
        return ResponseEntity.ok(ApiResponse.success("Dispatch stats retrieved successfully", result));
    }

    @Operation(summary = "Reject incoming dispatch", description = "Rejects a pending dispatch record, logging rejection reasons")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatch rejected successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch record not found")
    @PutMapping("/{dispatchId}/reject")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> rejectDispatch(
            @PathVariable @NotNull Long dispatchId,
            @RequestParam(required = false) String reason) {
        Long rejectedBy = SecurityUtils.getCurrentUserId();
        log.info("Rejecting Dispatch ID: {}, Rejected By: {}", dispatchId, rejectedBy);
        DispatchResponseDTO result = dispatchService.rejectDispatch(dispatchId, rejectedBy, reason);
        return ResponseEntity.ok(ApiResponse.success("Dispatch rejected successfully", result));
    }

    @Operation(summary = "Get dispatch items by product ID", description = "Lists received items and batch codes matching a product ID (with optional branch filter)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Items list retrieved successfully")
    @GetMapping("/product/{productId}/items")
    public ResponseEntity<ApiResponse<List<DispatchItemDTO>>> getDispatchItemsByProduct(
            @PathVariable @NotNull Long productId,
            @RequestParam(required = false) Long branchId) {
        log.info("Fetching Dispatch items for product: {}", productId);
        List<DispatchItemDTO> result = dispatchService.getDispatchItemsByProduct(productId, branchId);
        return ResponseEntity.ok(ApiResponse.success("Dispatch items retrieved successfully", result));
    }

    @Operation(summary = "Verify dispatch number uniqueness", description = "Checks whether a supplier dispatch reference number already exists in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Uniqueness check completed")
    @GetMapping("/check-number/{dispatchNo}")
    public ResponseEntity<ApiResponse<Boolean>> checkDispatchNumber(
            @PathVariable @NotNull String dispatchNo) {
        boolean exists = dispatchService.isDispatchNumberExists(dispatchNo);
        return ResponseEntity.ok(ApiResponse.success("Dispatch number check completed", exists));
    }

    @Operation(summary = "Get dispatches by supplier", description = "Lists incoming dispatches registered for a specific supplier ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatches list retrieved successfully")
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<DispatchResponseDTO>>> getDispatchesBySupplier(
            @PathVariable @NotNull Long supplierId) {
        log.info("Fetching Dispatches for supplier: {}", supplierId);
        DispatchFilterDTO filter = new DispatchFilterDTO();
        filter.setSupplierId(supplierId);
        List<DispatchResponseDTO> result = dispatchService.searchDispatches(filter);
        return ResponseEntity.ok(ApiResponse.success("Dispatches retrieved successfully", result));
    }

    @Operation(summary = "Get pending dispatches", description = "Lists dispatches awaiting verification/approval, with optional branch filtering")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending dispatches list retrieved successfully")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<DispatchResponseDTO>>> getPendingDispatches(
            @RequestParam(required = false) Long branchId) {
        log.info("Fetching pending Dispatches");
        DispatchFilterDTO filter = new DispatchFilterDTO();
        filter.setStatus("PENDING");
        if (branchId != null) {
            filter.setBranchId(branchId);
        }
        List<DispatchResponseDTO> result = dispatchService.searchDispatches(filter);
        return ResponseEntity.ok(ApiResponse.success("Pending Dispatches retrieved successfully", result));
    }

    @Operation(summary = "Export dispatches report PDF", description = "Generates and downloads a printable PDF report listing recent dispatches and delivery values")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF generated and returned successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to compile Jasper PDF template")
    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> getDispatchListPdf() {
        log.info("Generating Dispatch list PDF");
        try {
            byte[] pdfBytes = jasperReportService.generateDispatchListPdf();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dispatch_list.pdf")
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Failed to generate Dispatch list PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

