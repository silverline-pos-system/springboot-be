package com.silverline.erp.module.procurement.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.ProcessPaymentRequest;
import com.silverline.erp.module.inventory.dto.TransferToManagerRequest;
import com.silverline.erp.module.procurement.dto.GrnPaymentRequestDTO;
import com.silverline.erp.module.procurement.service.GrnPaymentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grn-payments")
@RequiredArgsConstructor
@Tag(name = "GRN Payment Requests", description = "APIs for the supplier payment workflow raised from posted GRNs")
public class GrnPaymentRequestController {

    private final GrnPaymentRequestService paymentRequestService;

    private Long getCurrentUserId() {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new com.silverline.erp.common.exception.UnauthorizedException("User is not authenticated");
        }
        return userId;
    }

    private Long getBranchIdFromParam(Long branchId) {
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID is required");
        }
        return branchId;
    }

    @Operation(summary = "Get payment requests by branch")
    @GetMapping("/branch")
    public ResponseEntity<ApiResponse<List<GrnPaymentRequestDTO>>> getPaymentRequestsByBranch(
            @RequestParam Long branchId) {
        List<GrnPaymentRequestDTO> requests = paymentRequestService.getPaymentRequestsByBranch(getBranchIdFromParam(branchId));
        return ResponseEntity.ok(ApiResponse.success("Payment requests fetched", requests));
    }

    @Operation(summary = "Get pending count by branch")
    @GetMapping("/branch/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingCount(@RequestParam Long branchId) {
        Long count = paymentRequestService.getPendingCountByBranch(getBranchIdFromParam(branchId));
        return ResponseEntity.ok(ApiResponse.success("Count fetched", Map.of("count", count)));
    }

    @Operation(summary = "Get payment requests by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<GrnPaymentRequestDTO>>> getPaymentRequestsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success("Payment requests fetched",
                paymentRequestService.getPaymentRequestsByStatus(status)));
    }

    @Operation(summary = "Get manager payment requests list")
    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<List<GrnPaymentRequestDTO>>> getManagerPaymentRequests() {
        return ResponseEntity.ok(ApiResponse.success("Manager payment requests fetched",
                paymentRequestService.getManagerPaymentRequests()));
    }

    @Operation(summary = "Get manager pending count")
    @GetMapping("/manager/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getManagerPendingCount() {
        Long count = paymentRequestService.getManagerPendingCount();
        return ResponseEntity.ok(ApiResponse.success("Count fetched", Map.of("count", count)));
    }

    @Operation(summary = "Get payment request by ID")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<GrnPaymentRequestDTO>> getPaymentRequestById(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success("Payment request fetched",
                paymentRequestService.getPaymentRequestById(requestId)));
    }

    @Operation(summary = "Transfer payment request to manager")
    @PostMapping("/{requestId}/transfer-to-manager")
    public ResponseEntity<ApiResponse<GrnPaymentRequestDTO>> transferToManager(
            @PathVariable Long requestId, @Valid @RequestBody TransferToManagerRequest request) {
        try {
            GrnPaymentRequestDTO result = paymentRequestService.transferToManager(requestId, request, getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.success("Request transferred to manager", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Process and approve payment request")
    @PostMapping("/{requestId}/process-payment")
    public ResponseEntity<ApiResponse<GrnPaymentRequestDTO>> processPayment(
            @PathVariable Long requestId, @Valid @RequestBody ProcessPaymentRequest request) {
        try {
            GrnPaymentRequestDTO result = paymentRequestService.processPayment(requestId, request, getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Reject payment request")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<GrnPaymentRequestDTO>> rejectRequest(
            @PathVariable Long requestId, @RequestBody Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason", "No reason provided");
            GrnPaymentRequestDTO result = paymentRequestService.rejectRequest(requestId, reason, getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.success("Request rejected", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
