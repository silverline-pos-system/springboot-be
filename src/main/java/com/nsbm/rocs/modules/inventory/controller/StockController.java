package com.nsbm.rocs.modules.inventory.controller;

import com.nsbm.rocs.shared.response.ApiResponse;

import com.nsbm.rocs.modules.inventory.dto.*;
import com.nsbm.rocs.modules.inventory.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllStock() {
        List<StockDTO> stocks = stockService.getAllStock();
        return ResponseEntity.ok(ApiResponse.success("Stock retrieved successfully", stocks));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<?>> getStockByBranch(@PathVariable Long branchId) {
        List<StockDTO> stocks = stockService.getStockByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stock retrieved successfully", stocks));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getStockByProduct(@PathVariable Long productId) {
        List<StockDTO> stocks = stockService.getStockByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Stock retrieved successfully", stocks));
    }

    @GetMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getStockByBranchAndProduct(
            @PathVariable Long branchId,
            @PathVariable Long productId) {
        StockDTO stock = stockService.getStockByBranchAndProduct(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Stock retrieved successfully", stock));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<?>> adjustStock(@Valid @RequestBody StockAdjustmentDTO adjustmentDTO) {
        StockDTO stock = stockService.adjustStock(adjustmentDTO);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", stock));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> addStock(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        StockDTO stock = stockService.addStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock added successfully", stock));
    }

    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<?>> removeStock(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        StockDTO stock = stockService.removeStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock removed successfully", stock));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<?>> reserveStock(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        StockDTO stock = stockService.reserveStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock reserved successfully", stock));
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<?>> releaseReservedStock(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        StockDTO stock = stockService.releaseReservedStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Reserved stock released successfully", stock));
    }

    @GetMapping("/report")
    public ResponseEntity<ApiResponse<?>> getStockReport(
            @RequestParam(required = false) Long branchId) {
        List<StockReportDTO> report = stockService.getStockReport(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stock report generated successfully", report));
    }

    @GetMapping("/alerts/low-stock")
    public ResponseEntity<ApiResponse<?>> getLowStockAlerts(
            @RequestParam(required = false) Long branchId) {
        List<LowStockAlertDTO> alerts = stockService.getLowStockAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Low stock alerts retrieved successfully", alerts));
    }
}


