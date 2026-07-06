package com.silverline.erp.module.procurement.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.infrastructure.reporting.JasperReportService;
import com.silverline.erp.module.procurement.dto.*;
import com.silverline.erp.module.procurement.service.DispatchService;
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
public class DispatchController {

    private final DispatchService dispatchService;
    private final JasperReportService jasperReportService;

    @PostMapping
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> createDispatch(
            @Valid @RequestBody DispatchCreateRequestDTO request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Creating Dispatch, User: {}", currentUserId);
        DispatchResponseDTO result = dispatchService.createDispatch(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dispatch created successfully", result));
    }

    @GetMapping("/{dispatchId}")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> getDispatchById(
            @PathVariable @NotNull Long dispatchId) {
        log.info("Fetching Dispatch ID: {}", dispatchId);
        DispatchResponseDTO result = dispatchService.getDispatchById(dispatchId);
        return ResponseEntity.ok(ApiResponse.success("Dispatch retrieved successfully", result));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<DispatchResponseDTO>>> getDispatchesByBranch(
            @PathVariable @NotNull Long branchId) {
        log.info("Fetching Dispatches for branch: {}", branchId);
        List<DispatchResponseDTO> result = dispatchService.getDispatchesByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Dispatches retrieved successfully", result));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<DispatchResponseDTO>>> searchDispatches(
            @RequestBody DispatchFilterDTO filter) {
        log.info("Searching Dispatches");
        List<DispatchResponseDTO> result = dispatchService.searchDispatches(filter);
        return ResponseEntity.ok(ApiResponse.success("Dispatches retrieved successfully", result));
    }

    @PutMapping("/{dispatchId}")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> updateDispatch(
            @PathVariable @NotNull Long dispatchId,
            @Valid @RequestBody DispatchUpdateRequestDTO request) {
        log.info("Updating Dispatch ID: {}", dispatchId);
        DispatchResponseDTO result = dispatchService.updateDispatch(dispatchId, request);
        return ResponseEntity.ok(ApiResponse.success("Dispatch updated successfully", result));
    }

    @PutMapping("/{dispatchId}/approve")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> approveDispatch(
            @PathVariable @NotNull Long dispatchId) {
        Long approvedBy = SecurityUtils.getCurrentUserId();
        log.info("Approving Dispatch ID: {}, Approved By: {}", dispatchId, approvedBy);
        DispatchResponseDTO result = dispatchService.approveDispatch(dispatchId, approvedBy);
        return ResponseEntity.ok(ApiResponse.success("Dispatch approved successfully", result));
    }

    @PutMapping("/{dispatchId}/payment-status")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> updatePaymentStatus(
            @PathVariable @NotNull Long dispatchId,
            @RequestParam String paymentStatus) {
        log.info("Updating payment status for Dispatch ID: {} to {}", dispatchId, paymentStatus);
        DispatchResponseDTO result = dispatchService.updatePaymentStatus(dispatchId, paymentStatus);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", result));
    }

    @DeleteMapping("/{dispatchId}")
    public ResponseEntity<ApiResponse<Void>> deleteDispatch(
            @PathVariable @NotNull Long dispatchId) {
        log.info("Deleting Dispatch ID: {}", dispatchId);
        dispatchService.deleteDispatch(dispatchId);
        return ResponseEntity.ok(ApiResponse.success("Dispatch deleted successfully"));
    }

    @GetMapping("/branch/{branchId}/stats")
    public ResponseEntity<ApiResponse<DispatchStatsDTO>> getDispatchStats(
            @PathVariable @NotNull Long branchId,
            @RequestParam(required = false) String period) {
        log.info("Fetching Dispatch stats for branch: {}, period: {}", branchId, period);
        DispatchStatsDTO result = dispatchService.getDispatchStats(branchId, period);
        return ResponseEntity.ok(ApiResponse.success("Dispatch stats retrieved successfully", result));
    }

    @PutMapping("/{dispatchId}/reject")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> rejectDispatch(
            @PathVariable @NotNull Long dispatchId,
            @RequestParam(required = false) String reason) {
        Long rejectedBy = SecurityUtils.getCurrentUserId();
        log.info("Rejecting Dispatch ID: {}, Rejected By: {}", dispatchId, rejectedBy);
        DispatchResponseDTO result = dispatchService.rejectDispatch(dispatchId, rejectedBy, reason);
        return ResponseEntity.ok(ApiResponse.success("Dispatch rejected successfully", result));
    }

    @GetMapping("/product/{productId}/items")
    public ResponseEntity<ApiResponse<List<DispatchItemDTO>>> getDispatchItemsByProduct(
            @PathVariable @NotNull Long productId,
            @RequestParam(required = false) Long branchId) {
        log.info("Fetching Dispatch items for product: {}", productId);
        List<DispatchItemDTO> result = dispatchService.getDispatchItemsByProduct(productId, branchId);
        return ResponseEntity.ok(ApiResponse.success("Dispatch items retrieved successfully", result));
    }

    @GetMapping("/check-number/{dispatchNo}")
    public ResponseEntity<ApiResponse<Boolean>> checkDispatchNumber(
            @PathVariable @NotNull String dispatchNo) {
        boolean exists = dispatchService.isDispatchNumberExists(dispatchNo);
        return ResponseEntity.ok(ApiResponse.success("Dispatch number check completed", exists));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<DispatchResponseDTO>>> getDispatchesBySupplier(
            @PathVariable @NotNull Long supplierId) {
        log.info("Fetching Dispatches for supplier: {}", supplierId);
        DispatchFilterDTO filter = new DispatchFilterDTO();
        filter.setSupplierId(supplierId);
        List<DispatchResponseDTO> result = dispatchService.searchDispatches(filter);
        return ResponseEntity.ok(ApiResponse.success("Dispatches retrieved successfully", result));
    }

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
