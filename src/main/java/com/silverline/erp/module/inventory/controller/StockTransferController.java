package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.module.inventory.dto.StockTransferRequestDTO;
import com.silverline.erp.module.inventory.dto.StockTransferResponseDTO;
import com.silverline.erp.module.inventory.service.StockTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/transfers", "/api/inventory/transfers"})
@Validated
@RequiredArgsConstructor
@Tag(name = "Stock Transfers", description = "APIs for creating, submitting, approving, and rejecting inventory stock transfers between store branches")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @Operation(summary = "Create stock transfer draft", description = "Creates a new stock transfer request draft between source and destination branches")
    @ApiResponse(responseCode = "200", description = "Stock transfer draft created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payload or validation error")
    @PostMapping
    public ResponseEntity<StockTransferResponseDTO> createTransfer(@Valid @RequestBody StockTransferRequestDTO request) {
        StockTransferResponseDTO response = stockTransferService.createTransfer(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get stock transfers", description = "Retrieves a list of stock transfers matching status, branches, or requested by filters")
    @ApiResponse(responseCode = "200", description = "Transfers list retrieved successfully")
    @GetMapping
    public ResponseEntity<List<StockTransferResponseDTO>> getTransfers(@RequestParam(required = false) String status,
                                                                       @RequestParam(required = false) Long fromBranchId,
                                                                       @RequestParam(required = false) Long toBranchId,
                                                                       @RequestParam(required = false) Long requestedBy) {
        List<StockTransferResponseDTO> response = stockTransferService.getTransfers(status, fromBranchId, toBranchId, requestedBy);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get stock transfer by ID", description = "Retrieves stock transfer parameters and item lists by database ID")
    @ApiResponse(responseCode = "200", description = "Stock transfer details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Stock transfer record not found")
    @GetMapping("/{id}")
    public ResponseEntity<StockTransferResponseDTO> getTransferById(@PathVariable Long id) {
        StockTransferResponseDTO response = stockTransferService.getTransferById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update stock transfer draft", description = "Modifies items or quantities of a draft stock transfer")
    @ApiResponse(responseCode = "200", description = "Stock transfer draft updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid transfer state or item values")
    @ApiResponse(responseCode = "404", description = "Stock transfer record not found")
    @PutMapping("/{id}")
    public ResponseEntity<StockTransferResponseDTO> updateTransfer(@PathVariable Long id, @Valid @RequestBody StockTransferRequestDTO request) {
        StockTransferResponseDTO response = stockTransferService.updateTransfer(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit stock transfer", description = "Submits a draft stock transfer, transitioning status to PENDING and reserving stock count in the source branch")
    @ApiResponse(responseCode = "200", description = "Stock transfer submitted successfully")
    @ApiResponse(responseCode = "400", description = "Insufficient stock at source branch or invalid draft state")
    @ApiResponse(responseCode = "404", description = "Stock transfer record not found")
    @PostMapping("/{id}/submit")
    public ResponseEntity<String> submitTransfer(@PathVariable Long id) {
        String response = stockTransferService.submitTransfer(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Approve stock transfer", description = "Approves a pending stock transfer, adjusting inventories at both source and destination branches")
    @ApiResponse(responseCode = "200", description = "Stock transfer approved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid transfer state or approval validation error")
    @ApiResponse(responseCode = "404", description = "Stock transfer record not found")
    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approveTransfer(@PathVariable Long id, @RequestParam(required = false) String approvalNotes) {
        String response = stockTransferService.approveTransfer(id, approvalNotes);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject stock transfer", description = "Rejects a pending stock transfer, releasing the reserved stock count at the source branch back to available counts")
    @ApiResponse(responseCode = "200", description = "Stock transfer rejected successfully")
    @ApiResponse(responseCode = "400", description = "Invalid transfer state or rejection validation error")
    @ApiResponse(responseCode = "404", description = "Stock transfer record not found")
    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectTransfer(@PathVariable Long id, @RequestParam(required = false) String rejectionReason) {
        String response = stockTransferService.rejectTransfer(id, rejectionReason);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete stock transfer", description = "Deletes a draft stock transfer record")
    @ApiResponse(responseCode = "200", description = "Stock transfer deleted successfully")
    @ApiResponse(responseCode = "400", description = "Cannot delete a non-draft stock transfer")
    @ApiResponse(responseCode = "404", description = "Stock transfer record not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTransfer(@PathVariable Long id) {
        String response = stockTransferService.deleteTransfer(id);
        return ResponseEntity.ok(response);
    }
}
