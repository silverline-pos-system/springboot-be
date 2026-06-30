package com.nsbm.rocs.modules.inventory.controller;

import com.nsbm.rocs.shared.response.ApiResponse;
import com.nsbm.rocs.entity.main.UserProfile;
import com.nsbm.rocs.modules.inventory.dto.DispatchPaymentRequestDTO;
import com.nsbm.rocs.modules.inventory.dto.ProcessPaymentRequest;
import com.nsbm.rocs.modules.inventory.dto.TransferToManagerRequest;
import com.nsbm.rocs.modules.inventory.service.DispatchPaymentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dispatch-payments")
@CrossOrigin
@RequiredArgsConstructor
public class DispatchPaymentRequestController {

    private final DispatchPaymentRequestService paymentRequestService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return ((UserProfile) auth.getPrincipal()).getUserId();
        }
        return 1L; // Default fallback
    }

    // NOTE: branchId now from request params — users are NOT tied to branches
    private Long getBranchIdFromParam(Long branchId) {
        return branchId != null ? branchId : 1L;
    }

    /**
     * Get payment requests for current branch (for POS display)
     */
    @GetMapping("/branch")
    public ResponseEntity<ApiResponse<List<DispatchPaymentRequestDTO>>> getPaymentRequestsByBranch(
            @RequestParam(required = false) Long branchId) {
        Long targetBranchId = getBranchIdFromParam(branchId);
        List<DispatchPaymentRequestDTO> requests = paymentRequestService.getPaymentRequestsByBranch(targetBranchId);
        return ResponseEntity.ok(ApiResponse.success("Payment requests fetched", requests));
    }

    /**
     * Get pending payment requests count for POS notification badge
     */
    @GetMapping("/branch/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingCount(
            @RequestParam(required = false) Long branchId) {
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
            @RequestBody TransferToManagerRequest request) {
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
            @RequestBody ProcessPaymentRequest request) {
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
