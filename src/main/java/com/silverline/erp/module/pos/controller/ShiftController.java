package com.silverline.erp.module.pos.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.pos.dto.ShiftStartRequest;
import com.silverline.erp.module.pos.dto.shift.CloseShiftRequest;
import com.silverline.erp.module.pos.service.CashReconciliationService;
import com.silverline.erp.module.pos.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pos")
@CrossOrigin // Added to fix CORS issues
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private CashReconciliationService cashReconciliationService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return ((UserProfile) auth.getPrincipal()).getUserId();
        }
        return 1001L; // Keeping fallback for safety during dev, but ideally remove
    }

    @PostMapping("/shift/open")
    public ResponseEntity<?> startShift(@Valid @RequestBody ShiftStartRequest request) {
        try {
            Long shiftId = shiftService.startShift(request);
            return ResponseEntity.ok(Map.of("shiftId", shiftId, "status", "Shift opened successfully"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal Server Error: " + e.getMessage()));
        }
    }

    @GetMapping("/shift/active")
    public ResponseEntity<ApiResponse<com.silverline.erp.module.pos.dto.shift.ShiftResponse>> getActiveShift(
            @RequestParam(required = false) Long branchId, 
            @RequestParam(required = false) Long cashierId) {
        try {
            com.silverline.erp.module.pos.dto.shift.ShiftResponse activeShift = shiftService.getActiveShift(branchId, cashierId);
            if (activeShift == null) {
                 return ResponseEntity.status(404).body(ApiResponse.error("No active shift found"));
            }
            return ResponseEntity.ok(ApiResponse.success("Active shift found", activeShift));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping({"/shift/{shiftId}/totals", "/shifts/{shiftId}/totals"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShiftTotals(@PathVariable Long shiftId) {
        try {
            Map<String, Object> totals = cashReconciliationService.getShiftTotals(shiftId);
            return ResponseEntity.ok(ApiResponse.success("Shift totals", totals));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/shift/close")
    public ResponseEntity<ApiResponse<String>> closeShift(@Valid @RequestBody CloseShiftRequest request) {
        Long cashierId;
        try {
             cashierId = getCurrentUserId();
        } catch (Exception e) {
             cashierId = 1001L;
        }

        try {
            shiftService.closeShift(cashierId, request);
            return ResponseEntity.ok(ApiResponse.success("Shift closed successfully", "Shift closed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/shift/{shiftId}/close")
    public ResponseEntity<ApiResponse<String>> closeShiftById(@PathVariable Long shiftId, @Valid @RequestBody CloseShiftRequest request) {
        try {
            shiftService.closeShiftById(shiftId, request);
            return ResponseEntity.ok(ApiResponse.success("Shift closed successfully", "Shift closed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cashiers")
    public ResponseEntity<ApiResponse<java.util.List<com.silverline.erp.domain.user.UserProfile>>> getCashiers(@RequestParam Long branchId) {
        try {
            java.util.List<com.silverline.erp.domain.user.UserProfile> cashiers = shiftService.getCashiersByBranch(branchId);
            return ResponseEntity.ok(ApiResponse.success("Cashiers retrieved", cashiers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/cash-flows")
    public ResponseEntity<ApiResponse<com.silverline.erp.domain.pos.CashFlow>> recordCashFlow(@Valid @RequestBody com.silverline.erp.module.pos.dto.CashFlowRequest request) {
        Long cashierId;
        try {
             cashierId = getCurrentUserId();
        } catch (Exception e) {
             cashierId = 1001L;
        }
        try {
            com.silverline.erp.domain.pos.CashFlow flow = cashReconciliationService.recordCashFlow(cashierId, request);
            return ResponseEntity.ok(ApiResponse.success("Cash flow recorded", flow));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cash-flows/shift/{shiftId}")
    public ResponseEntity<ApiResponse<java.util.List<com.silverline.erp.domain.pos.CashFlow>>> getCashFlowsByShift(@PathVariable Long shiftId) {
        try {
            java.util.List<com.silverline.erp.domain.pos.CashFlow> flows = cashReconciliationService.getShiftCashFlows(shiftId);
            return ResponseEntity.ok(ApiResponse.success("Cash flows retrieved", flows));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}


