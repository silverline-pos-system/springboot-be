package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.StockAdjustmentDTO;
import com.silverline.erp.module.inventory.service.AdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/adjustments", "/api/inventory/adjustments"})
@RequiredArgsConstructor
@Tag(name = "Stock Level Audits", description = "APIs for logging physical stock audits and correcting inventory counts (adjustments)")
public class AdjustmentController {

    private final AdjustmentService adjustmentService;

    @Operation(summary = "Get all stock adjustments", description = "Retrieves a list of all logged stock adjustments, with optional filters for branch location and product ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Adjustments retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllAdjustments(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long productId) {
        List<StockAdjustmentDTO> adjustments = adjustmentService.getAllAdjustments(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Adjustments retrieved successfully", adjustments));
    }

    @Operation(summary = "Create a stock adjustment", description = "Logs a new physical inventory reconciliation audit, updating the corresponding stock count")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Stock adjustment logged and updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or insufficient available inventory for negative adjustment")
    @PostMapping
    @PreAuthorize("hasAnyRole('CASHIER','SUPERVISOR','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> createAdjustment(@Valid @RequestBody StockAdjustmentDTO adjustmentDTO) {
        StockAdjustmentDTO created = adjustmentService.createAdjustment(adjustmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Adjustment created successfully", created));
    }
}

