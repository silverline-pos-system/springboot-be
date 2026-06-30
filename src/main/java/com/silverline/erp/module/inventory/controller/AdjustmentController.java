package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;

import com.silverline.erp.module.inventory.dto.StockAdjustmentDTO;
import com.silverline.erp.module.inventory.service.AdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/adjustments")
@RequiredArgsConstructor
public class AdjustmentController {

    private final AdjustmentService adjustmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllAdjustments(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long productId) {
        List<StockAdjustmentDTO> adjustments = adjustmentService.getAllAdjustments(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Adjustments retrieved successfully", adjustments));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createAdjustment(@Valid @RequestBody StockAdjustmentDTO adjustmentDTO) {
        StockAdjustmentDTO created = adjustmentService.createAdjustment(adjustmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Adjustment created successfully", created));
    }
}



