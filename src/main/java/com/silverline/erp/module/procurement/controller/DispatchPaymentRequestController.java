package com.silverline.erp.module.procurement.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.ProcessPaymentRequest;
import com.silverline.erp.module.inventory.dto.TransferToManagerRequest;
import com.silverline.erp.module.procurement.dto.DispatchPaymentRequestDTO;
import com.silverline.erp.module.procurement.service.DispatchPaymentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dispatch-payments")
@RequiredArgsConstructor
public class DispatchPaymentRequestController {

    private final DispatchPaymentRequestService paymentRequestService;

    private Long getCurrentUserId() {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("User is not authenticated");
        }
        return userId;
    }

    // NOTE: branchId now from request params — users are NOT tied to branches
    private Long getBranchIdFromParam(Long branchId) {
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID is required");
        }
        return branchId;
    }

    /**
     * Get payment requests for current branch (for POS display)
     */
    @GetMapping("/branch")
    public ResponseEntity<ApiResponse<List<DispatchPaymentRequestDTO>>> getPaymentRequestsByBranch(
            @RequestParam(required = true) Long branchId) {
        Long targetBranchId = getBranchIdFromParam(branchId);
        List<DispatchPaymentRequestDTO> requests = paymentRequestService.getPaymentRequestsByBranch(targetBranchId);
        return ResponseEntity.ok(ApiResponse.success("Payment requests fetched", requests));
    }

    /**
     * Get pending payment requests count for POS notification badge
     */
    @GetMapping("/branch/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingCount(
            @RequestParam(required = true) Long branchId) {
        Long targetBranchId = getBranchIdFromParam(branchId);
        Long count = paymentRequestService.getPendingCountByBranch(targetBranchId);
        return ResponseEntity.ok(ApiResponse.success("Count fetched", Map.of("count", count)));
    }

    /**
     * Get payment requests by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<DispatchPaymentRequestDTO>>> getPaymentRequestsByStatus(
            @PathVariable String status) {
        List<DispatchPaymentRequestDTO> requests = paymentRequestService.getPaymentRequestsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Payment requests fetched", requests));
    }

    /**
     * Get payment requests for manager (transferred ones)
     */
    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<List<DispatchPaymentRequestDTO>>> getManagerPaymentRequests() {
        List<DispatchPaymentRequestDTO> requests = paymentRequestService.getManagerPaymentRequests();
        return ResponseEntity.ok(ApiResponse.success("Manager payment requests fetched", requests));
    }

    /**
     * Get manager pending count for notification
     */
    @GetMapping("/manager/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getManagerPendingCount() {
        Long count = paymentRequestService.getManagerPendingCount();
        return ResponseEntity.ok(ApiResponse.success("Count fetched", Map.of("count", count)));
    }

    /**
     * Get single payment request by ID
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<DispatchPaymentRequestDTO>> getPaymentRequestById(
            @PathVariable Long requestId) {
        DispatchPaymentRequestDTO request = paymentRequestService.getPaymentRequestById(requestId);
        return ResponseEntity.ok(ApiResponse.success("Payment request fetched", request));
    }

    /**
     * Transfer payment request to manager (requires supervisor approval)
     */
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

    /**
     * Manager processes the payment
     */
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

    /**
     * Reject a payment request
     */
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
