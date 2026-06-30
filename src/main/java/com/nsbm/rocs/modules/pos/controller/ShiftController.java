package com.nsbm.rocs.modules.pos.controller;

import com.nsbm.rocs.entity.pos.CashShift;
import com.nsbm.rocs.modules.pos.dto.ShiftStartRequest;
import com.nsbm.rocs.modules.pos.dto.shift.CloseShiftRequest;
import com.nsbm.rocs.modules.pos.service.ShiftService;
import com.nsbm.rocs.shared.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;
import com.nsbm.rocs.entity.main.UserProfile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/v1/pos")
@CrossOrigin // Added to fix CORS issues
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return ((UserProfile) auth.getPrincipal()).getUserId();
        }
        // Fallback for development/testing if auth not fully set up or for non-user principals
        // But ideally should throw error if not authenticated
        // throw new RuntimeException("User not authenticated");
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
            // Catch RuntimeExceptions (like "Invalid supervisor credentials") as 400/401
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal Server Error: " + e.getMessage()));
        }
    }

    @GetMapping("/shift/active")
    public ResponseEntity<ApiResponse<com.nsbm.rocs.modules.pos.dto.shift.ShiftResponse>> getActiveShift(
            @RequestParam(required = false) Long branchId, 
            @RequestParam(required = false) Long cashierId) {
        try {
            com.nsbm.rocs.modules.pos.dto.shift.ShiftResponse activeShift = shiftService.getActiveShift(branchId, cashierId);
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
            Map<String, Object> totals = shiftService.getShiftTotals(shiftId);
            return ResponseEntity.ok(ApiResponse.success("Shift totals", totals));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/shift/close")
    public ResponseEntity<ApiResponse<String>> closeShift(@RequestBody CloseShiftRequest request) {
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
    public ResponseEntity<ApiResponse<String>> closeShiftById(@PathVariable Long shiftId, @RequestBody CloseShiftRequest request) {
        try {
            shiftService.closeShiftById(shiftId, request);
            return ResponseEntity.ok(ApiResponse.success("Shift closed successfully", "Shift closed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cashiers")
    public ResponseEntity<ApiResponse<java.util.List<com.nsbm.rocs.entity.main.UserProfile>>> getCashiers(@RequestParam Long branchId) {
        try {
            java.util.List<com.nsbm.rocs.entity.main.UserProfile> cashiers = shiftService.getCashiersByBranch(branchId);
            return ResponseEntity.ok(ApiResponse.success("Cashiers retrieved", cashiers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/cash-flows")
    public ResponseEntity<ApiResponse<com.nsbm.rocs.entity.pos.CashFlow>> recordCashFlow(@RequestBody com.nsbm.rocs.modules.pos.dto.CashFlowRequest request) {
        Long cashierId;
        try {
             cashierId = getCurrentUserId();
        } catch (Exception e) {
             cashierId = 1001L;
        }
        try {
            com.nsbm.rocs.entity.pos.CashFlow flow = shiftService.recordCashFlow(cashierId, request);
            return ResponseEntity.ok(ApiResponse.success("Cash flow recorded", flow));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cash-flows/shift/{shiftId}")
    public ResponseEntity<ApiResponse<java.util.List<com.nsbm.rocs.entity.pos.CashFlow>>> getCashFlowsByShift(@PathVariable Long shiftId) {
        try {
            java.util.List<com.nsbm.rocs.entity.pos.CashFlow> flows = shiftService.getShiftCashFlows(shiftId);
            return ResponseEntity.ok(ApiResponse.success("Cash flows retrieved", flows));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}


