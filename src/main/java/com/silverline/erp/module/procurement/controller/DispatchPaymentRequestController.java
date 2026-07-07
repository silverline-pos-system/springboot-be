package com.silverline.erp.module.procurement.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.ProcessPaymentRequest;
import com.silverline.erp.module.inventory.dto.TransferToManagerRequest;
import com.silverline.erp.module.procurement.dto.DispatchPaymentRequestDTO;
import com.silverline.erp.module.procurement.service.DispatchPaymentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dispatch-payments")
@RequiredArgsConstructor
@Tag(name = "Dispatch Payment Requests", description = "APIs for cashiers to request dispatch cash payouts from POS registers, and managers to verify or override payouts")
public class DispatchPaymentRequestController {

    private final DispatchPaymentRequestService paymentRequestService;

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

    @Operation(summary = "Get payment requests by branch", description = "Retrieves a list of dispatch payment requests for the specified branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment requests list retrieved successfully")
    @GetMapping("/branch")
    public ResponseEntity<ApiResponse<List<DispatchPaymentRequestDTO>>> getPaymentRequestsByBranch(
            @RequestParam(required = true) Long branchId) {
        Long targetBranchId = getBranchIdFromParam(branchId);
        List<DispatchPaymentRequestDTO> requests = paymentRequestService.getPaymentRequestsByBranch(targetBranchId);
        return ResponseEntity.ok(ApiResponse.success("Payment requests fetched", requests));
    }

    @Operation(summary = "Get pending count by branch", description = "Retrieves the pending payment requests count (used for frontend navigation alerts)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    @GetMapping("/branch/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingCount(
            @RequestParam(required = true) Long branchId) {
        Long targetBranchId = getBranchIdFromParam(branchId);
        Long count = paymentRequestService.getPendingCountByBranch(targetBranchId);
        return ResponseEntity.ok(ApiResponse.success("Count fetched", Map.of("count", count)));
    }

    @Operation(summary = "Get payment requests by status", description = "Retrieves requests matching a specific status code (e.g. PENDING, APPROVED, TRANSFERRED)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment requests list retrieved successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<DispatchPaymentRequestDTO>>> getPaymentRequestsByStatus(
            @PathVariable String status) {
        List<DispatchPaymentRequestDTO> requests = paymentRequestService.getPaymentRequestsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Payment requests fetched", requests));
    }

    @Operation(summary = "Get manager payment requests list", description = "Retrieves all payment requests transferred to the manager dashboard")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Manager requests list retrieved successfully")
    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<List<DispatchPaymentRequestDTO>>> getManagerPaymentRequests() {
        List<DispatchPaymentRequestDTO> requests = paymentRequestService.getManagerPaymentRequests();
        return ResponseEntity.ok(ApiResponse.success("Manager payment requests fetched", requests));
    }

    @Operation(summary = "Get manager pending count", description = "Retrieves the count of pending payment requests waiting on the manager dashboard")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    @GetMapping("/manager/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getManagerPendingCount() {
        Long count = paymentRequestService.getManagerPendingCount();
        return ResponseEntity.ok(ApiResponse.success("Count fetched", Map.of("count", count)));
    }

    @Operation(summary = "Get payment request by ID", description = "Retrieves a single dispatch payment request by database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment request details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Request not found")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<DispatchPaymentRequestDTO>> getPaymentRequestById(
            @PathVariable Long requestId) {
        DispatchPaymentRequestDTO request = paymentRequestService.getPaymentRequestById(requestId);
        return ResponseEntity.ok(ApiResponse.success("Payment request fetched", request));
    }

    @Operation(summary = "Transfer payment request to manager", description = "Escalates a payment request to the manager override dashboard (requires supervisor credential authorization)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request escalated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Authorization failure or invalid request state")
    @PostMapping("/{requestId}/transfer-to-manager")
    public ResponseEntity<ApiResponse<DispatchPaymentRequestDTO>> transferToManager(
            @PathVariable Long requestId,
            @Valid @RequestBody TransferToManagerRequest request) {
        try {
            Long userId = getCurrentUserId();
            DispatchPaymentRequestDTO result = paymentRequestService.transferToManager(requestId, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Request transferred to manager", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Process and approve payment request", description = "Manager processes and pays out the dispatch request, updating cash flow registers and ledger logs")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment processed and paid successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient register cash flow or invalid request state")
    @PostMapping("/{requestId}/process-payment")
    public ResponseEntity<ApiResponse<DispatchPaymentRequestDTO>> processPayment(
            @PathVariable Long requestId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        try {
            Long userId = getCurrentUserId();
            DispatchPaymentRequestDTO result = paymentRequestService.processPayment(requestId, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Reject payment request", description = "Rejects a cashier's dispatch payment request, logging rejection reason")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment request rejected successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request state or rejection validation failure")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<DispatchPaymentRequestDTO>> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body) {
        try {
            Long userId = getCurrentUserId();
            String reason = body.getOrDefault("reason", "No reason provided");
            DispatchPaymentRequestDTO result = paymentRequestService.rejectRequest(requestId, reason, userId);
            return ResponseEntity.ok(ApiResponse.success("Request rejected", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

