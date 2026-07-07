package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.inventory.dto.BatchDTO;
import com.silverline.erp.module.inventory.dto.ExpiryAlertDTO;
import com.silverline.erp.module.inventory.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/batches", "/api/inventory/batches"})
@RequiredArgsConstructor
@Tag(name = "Product Batches", description = "APIs for registering and tracking specific product batches, manufactured/expiry dates, FEFO (First Expired First Out) listings, and alerts")
public class BatchController {

    private final BatchService batchService;

    @Operation(summary = "Get all batches", description = "Retrieves a paginated list of all batches registered in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batches list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BatchDTO>>> getAllBatches(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BatchDTO> pageInfo = batchService.getAllBatches(pageable);
        return ResponseEntity.ok(ApiResponse.success("Batches retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get batches by product ID", description = "Lists all batches registered for a specific product")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batches list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getBatchesByProduct(@PathVariable Long productId) {
        List<BatchDTO> batches = batchService.getBatchesByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Batches retrieved successfully", batches));
    }

    @Operation(summary = "Get batches by branch ID", description = "Lists all batches currently stored at a specific branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batches list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Branch not found")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<?>> getBatchesByBranch(@PathVariable Long branchId) {
        List<BatchDTO> batches = batchService.getBatchesByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Batches retrieved successfully", batches));
    }

    @Operation(summary = "Get batch by ID", description = "Retrieves batch details including code, cost price, and dates by batch database ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Batch not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getBatchById(@PathVariable Long id) {
        BatchDTO batch = batchService.getBatchById(id);
        return ResponseEntity.ok(ApiResponse.success("Batch retrieved successfully", batch));
    }

    @Operation(summary = "Get batch by unique code combination", description = "Looks up a specific batch matching a branch ID, product ID, and batch code string")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Batch combination not found")
    @GetMapping("/branch/{branchId}/product/{productId}/code/{batchCode}")
    public ResponseEntity<ApiResponse<?>> getBatchByCode(
            @PathVariable Long branchId,
            @PathVariable Long productId,
            @PathVariable String batchCode) {
        BatchDTO batch = batchService.getBatchByCode(branchId, productId, batchCode);
        return ResponseEntity.ok(ApiResponse.success("Batch retrieved successfully", batch));
    }

    @Operation(summary = "Get all expired batches", description = "Retrieves a list of all batches whose expiry dates are prior to today's date")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expired batches list retrieved successfully")
    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<?>> getExpiredBatches() {
        List<BatchDTO> batches = batchService.getExpiredBatches();
        return ResponseEntity.ok(ApiResponse.success("Expired batches retrieved successfully", batches));
    }

    @Operation(summary = "Get batches expiring soon", description = "Retrieves a list of batches expiring within a threshold window of days (defaults to 30 days)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expiring batches list retrieved successfully")
    @GetMapping("/expiring-soon")
    public ResponseEntity<ApiResponse<?>> getExpiringSoonBatches(
            @RequestParam(defaultValue = "30") int days) {
        List<BatchDTO> batches = batchService.getExpiringSoonBatches(days);
        return ResponseEntity.ok(ApiResponse.success("Expiring batches retrieved successfully", batches));
    }

    @Operation(summary = "Register a new batch", description = "Creates a new batch record, defining purchase cost, quantity, and expiry dates")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Batch registered successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload configuration or duplicate batch code")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createBatch(@Valid @RequestBody BatchDTO batchDTO) {
        BatchDTO createdBatch = batchService.createBatch(batchDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch created successfully", createdBatch));
    }

    @Operation(summary = "Update batch details", description = "Updates dates, cost, or counts for a specific batch ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch details updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Batch not found")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody BatchDTO batchDTO) {
        BatchDTO updatedBatch = batchService.updateBatch(id, batchDTO);
        return ResponseEntity.ok(ApiResponse.success("Batch updated successfully", updatedBatch));
    }

    @Operation(summary = "Delete batch record", description = "Removes a batch record from the registry database")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Batch not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteBatch(@PathVariable Long id) {
        batchService.deleteBatch(id);
        return ResponseEntity.ok(ApiResponse.success("Batch deleted successfully"));
    }

    @Operation(summary = "Get expiry alerts", description = "Generates warning/critical status alerts for batches based on threshold days")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expiry alerts list compiled successfully")
    @GetMapping("/alerts/expiry")
    public ResponseEntity<ApiResponse<?>> getExpiryAlerts(
            @RequestParam(defaultValue = "30") int warningDays,
            @RequestParam(defaultValue = "7") int criticalDays) {
        List<ExpiryAlertDTO> alerts = batchService.getExpiryAlerts(warningDays, criticalDays);
        return ResponseEntity.ok(ApiResponse.success("Expiry alerts retrieved successfully", alerts));
    }

    @Operation(summary = "Get FEFO batches list", description = "Retrieves active non-expired batches with stock, sorted by earliest expiry first (First Expired, First Out) for a product and branch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FEFO batches list retrieved successfully")
    @GetMapping("/fefo")
    public ResponseEntity<ApiResponse<?>> getFEFOBatches(
            @RequestParam Long productId,
            @RequestParam Long branchId) {
        List<BatchDTO> batches = batchService.getFEFOBatches(productId, branchId);
        return ResponseEntity.ok(ApiResponse.success("FEFO batches retrieved successfully", batches));
    }
}

