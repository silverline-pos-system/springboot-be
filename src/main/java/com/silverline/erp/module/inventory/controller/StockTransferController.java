package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.module.inventory.dto.StockTransferRequestDTO;
import com.silverline.erp.module.inventory.dto.StockTransferResponseDTO;
import com.silverline.erp.module.inventory.service.StockTransferService;
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
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @PostMapping
    public ResponseEntity<StockTransferResponseDTO> createTransfer(@Valid @RequestBody StockTransferRequestDTO request) {
        StockTransferResponseDTO response = stockTransferService.createTransfer(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<StockTransferResponseDTO>> getTransfers(@RequestParam(required = false) String status,
                                                                       @RequestParam(required = false) Long fromBranchId,
                                                                       @RequestParam(required = false) Long toBranchId,
                                                                       @RequestParam(required = false) Long requestedBy) {
        List<StockTransferResponseDTO> response = stockTransferService.getTransfers(status, fromBranchId, toBranchId, requestedBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockTransferResponseDTO> getTransferById(@PathVariable Long id) {
        StockTransferResponseDTO response = stockTransferService.getTransferById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockTransferResponseDTO> updateTransfer(@PathVariable Long id, @Valid @RequestBody StockTransferRequestDTO request) {
        StockTransferResponseDTO response = stockTransferService.updateTransfer(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<String> submitTransfer(@PathVariable Long id) {
        String response = stockTransferService.submitTransfer(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approveTransfer(@PathVariable Long id, @RequestParam(required = false) String approvalNotes) {
        String response = stockTransferService.approveTransfer(id, approvalNotes);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectTransfer(@PathVariable Long id, @RequestParam(required = false) String rejectionReason) {
        String response = stockTransferService.rejectTransfer(id, rejectionReason);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTransfer(@PathVariable Long id) {
        String response = stockTransferService.deleteTransfer(id);
        return ResponseEntity.ok(response);
    }


}

