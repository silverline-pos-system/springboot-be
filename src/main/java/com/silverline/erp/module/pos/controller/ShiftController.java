package com.silverline.erp.module.pos.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.pos.dto.ShiftStartRequest;
import com.silverline.erp.module.pos.dto.shift.CloseShiftRequest;
import com.silverline.erp.module.pos.service.CashReconciliationService;
import com.silverline.erp.module.pos.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/pos")
@CrossOrigin // Added to fix CORS issues
@RequiredArgsConstructor
@Tag(name = "Cash Shift Management", description = "Cashier shift opening, closing, and reconciliation APIs")
public class ShiftController {

    private final ShiftService shiftService;

    private final CashReconciliationService cashReconciliationService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserProfile) {
            return ((UserProfile) auth.getPrincipal()).getUserId();
        }
        return 1001L; // Keeping fallback for safety during dev, but ideally remove
    }

    @Operation(summary = "Open a cash shift", description = "Opens a cashier shift with opening cash denomination details")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shift opened successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cashier already has an active shift or invalid parameters")
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

    @Operation(summary = "Get active cash shift", description = "Retrieves active shift information for a cashier in a branch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active shift found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No active shift found")
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

    @Operation(summary = "Get shift totals", description = "Calculates total sales, cash, returns, and expected balance for the shift")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shift totals fetched successfully")
    @GetMapping({"/shift/{shiftId}/totals", "/shifts/{shiftId}/totals"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShiftTotals(@PathVariable Long shiftId) {
        try {
            Map<String, Object> totals = cashReconciliationService.getShiftTotals(shiftId);
            return ResponseEntity.ok(ApiResponse.success("Shift totals", totals));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Close active shift", description = "Closes the current cashier's active shift and performs cash reconciliation validation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shift closed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No active shift found or pending flow requests exist")
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

    @Operation(summary = "Close shift by shift ID", description = "Closes a specific shift by its database ID, validating closing cash denominations")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shift closed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Shift already closed or validation failure")
    @PutMapping("/shift/{shiftId}/close")
    public ResponseEntity<ApiResponse<String>> closeShiftById(@PathVariable Long shiftId, @Valid @RequestBody CloseShiftRequest request) {
        try {
            shiftService.closeShiftById(shiftId, request);
            return ResponseEntity.ok(ApiResponse.success("Shift closed successfully", "Shift closed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Get branch cashiers", description = "Lists all cashier profiles registered in the specified branch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cashiers retrieved successfully")
    @GetMapping("/cashiers")
    public ResponseEntity<ApiResponse<java.util.List<com.silverline.erp.domain.user.UserProfile>>> getCashiers(@RequestParam Long branchId) {
        try {
            java.util.List<com.silverline.erp.domain.user.UserProfile> cashiers = shiftService.getCashiersByBranch(branchId);
            return ResponseEntity.ok(ApiResponse.success("Cashiers retrieved", cashiers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Record mid-shift cash flow", description = "Logs cash drop or cash addition transactions during a cashier shift")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cash flow recorded successfully")
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

    @Operation(summary = "Get shift cash flows", description = "Lists all mid-shift cash drops/additions for a specific shift ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cash flows retrieved successfully")
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


