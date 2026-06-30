package com.nsbm.rocs.modules.inventory.controller;

import com.nsbm.rocs.shared.response.ApiResponse;

import com.nsbm.rocs.modules.inventory.dto.StockAdjustmentDTO;
import com.nsbm.rocs.modules.inventory.service.AdjustmentService;
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



