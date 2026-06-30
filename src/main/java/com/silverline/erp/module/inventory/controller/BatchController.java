package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.common.dto.ApiResponse;

import com.silverline.erp.module.inventory.dto.BatchDTO;
import com.silverline.erp.module.inventory.dto.ExpiryAlertDTO;
import com.silverline.erp.module.inventory.service.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllBatches() {
        List<BatchDTO> batches = batchService.getAllBatches();
        return ResponseEntity.ok(ApiResponse.success("Batches retrieved successfully", batches));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getBatchesByProduct(@PathVariable Long productId) {
        List<BatchDTO> batches = batchService.getBatchesByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Batches retrieved successfully", batches));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<?>> getBatchesByBranch(@PathVariable Long branchId) {
        List<BatchDTO> batches = batchService.getBatchesByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Batches retrieved successfully", batches));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getBatchById(@PathVariable Long id) {
        BatchDTO batch = batchService.getBatchById(id);
        return ResponseEntity.ok(ApiResponse.success("Batch retrieved successfully", batch));
    }

    @GetMapping("/branch/{branchId}/product/{productId}/code/{batchCode}")
    public ResponseEntity<ApiResponse<?>> getBatchByCode(
            @PathVariable Long branchId,
            @PathVariable Long productId,
            @PathVariable String batchCode) {
        BatchDTO batch = batchService.getBatchByCode(branchId, productId, batchCode);
        return ResponseEntity.ok(ApiResponse.success("Batch retrieved successfully", batch));
    }

    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<?>> getExpiredBatches() {
        List<BatchDTO> batches = batchService.getExpiredBatches();
        return ResponseEntity.ok(ApiResponse.success("Expired batches retrieved successfully", batches));
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<ApiResponse<?>> getExpiringSoonBatches(
            @RequestParam(defaultValue = "30") int days) {
        List<BatchDTO> batches = batchService.getExpiringSoonBatches(days);
        return ResponseEntity.ok(ApiResponse.success("Expiring batches retrieved successfully", batches));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createBatch(@Valid @RequestBody BatchDTO batchDTO) {
        BatchDTO createdBatch = batchService.createBatch(batchDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch created successfully", createdBatch));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody BatchDTO batchDTO) {
        BatchDTO updatedBatch = batchService.updateBatch(id, batchDTO);
        return ResponseEntity.ok(ApiResponse.success("Batch updated successfully", updatedBatch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteBatch(@PathVariable Long id) {
        batchService.deleteBatch(id);
        return ResponseEntity.ok(ApiResponse.success("Batch deleted successfully"));
    }

    @GetMapping("/alerts/expiry")
    public ResponseEntity<ApiResponse<?>> getExpiryAlerts(
            @RequestParam(defaultValue = "30") int warningDays,
            @RequestParam(defaultValue = "7") int criticalDays) {
        List<ExpiryAlertDTO> alerts = batchService.getExpiryAlerts(warningDays, criticalDays);
        return ResponseEntity.ok(ApiResponse.success("Expiry alerts retrieved successfully", alerts));
    }

    /**
     * FEFO Batch endpoint for dispatch form.
     * Returns batches sorted by earliest expiry first (First Expired, First Out).
     * Only returns non-expired batches with qty > 0.
     */
    @GetMapping("/fefo")
    public ResponseEntity<ApiResponse<?>> getFEFOBatches(
            @RequestParam Long productId,
            @RequestParam Long branchId) {
        List<BatchDTO> batches = batchService.getFEFOBatches(productId, branchId);
        return ResponseEntity.ok(ApiResponse.success("FEFO batches retrieved successfully", batches));
    }
}


