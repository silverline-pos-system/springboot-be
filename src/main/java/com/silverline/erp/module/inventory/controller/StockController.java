package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.common.dto.PagedResponse;
import com.silverline.erp.module.inventory.dto.LowStockAlertDTO;
import com.silverline.erp.module.inventory.dto.StockAdjustmentDTO;
import com.silverline.erp.module.inventory.dto.StockDTO;
import com.silverline.erp.module.inventory.dto.StockReportDTO;
import com.silverline.erp.module.inventory.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/stock", "/api/inventory/stock"})
@RequiredArgsConstructor
@Tag(name = "Stock Adjustments", description = "APIs for adjusting stock, reserving item counts for pending checkout invoices, and retrieving low stock reports")
public class StockController {

    private final StockService stockService;

    @Operation(summary = "Get all stock entries", description = "Retrieves a paginated list of all active stock levels in the system")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StockDTO>>> getAllStock(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<StockDTO> pageInfo = stockService.getAllStock(pageable);
        return ResponseEntity.ok(ApiResponse.success("Stock retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get stock entries by branch ID", description = "Retrieves a paginated list of stock levels for a specific branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Branch not found")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<PagedResponse<StockDTO>>> getStockByBranch(
            @PathVariable Long branchId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<StockDTO> pageInfo = stockService.getStockByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Stock retrieved successfully", PagedResponse.from(pageInfo)));
    }

    @Operation(summary = "Get stock by product ID", description = "Lists stock records of a product across all branch locations")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock list retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getStockByProduct(@PathVariable Long productId) {
        List<StockDTO> stocks = stockService.getStockByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Stock retrieved successfully", stocks));
    }

    @Operation(summary = "Get stock by branch and product", description = "Retrieves the stock level of a specific product at a specific branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock level retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Branch or Product combination not found")
    @GetMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getStockByBranchAndProduct(
            @PathVariable Long branchId,
            @PathVariable Long productId) {
        StockDTO stock = stockService.getStockByBranchAndProduct(branchId, productId);
        return ResponseEntity.ok(ApiResponse.success("Stock retrieved successfully", stock));
    }

    @Operation(summary = "Adjust stock quantity", description = "Applies a manual quantity correction (reconciliation) on a branch stock entry")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock adjusted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload details or validation error")
    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('CASHIER','SUPERVISOR','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> adjustStock(@Valid @RequestBody StockAdjustmentDTO adjustmentDTO) {
        StockDTO stock = stockService.adjustStock(adjustmentDTO);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", stock));
    }

    @Operation(summary = "Increment stock quantity", description = "Increments available stock for a product in a branch by a target count")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock incremented successfully")
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('CASHIER','SUPERVISOR','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> addStock(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        StockDTO stock = stockService.addStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock added successfully", stock));
    }

    @Operation(summary = "Decrement stock quantity", description = "Decrements available stock for a product in a branch by a target count")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock decremented successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient stock available to decrement")
    @PostMapping("/remove")
    @PreAuthorize("hasAnyRole('CASHIER','SUPERVISOR','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> removeStock(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        StockDTO stock = stockService.removeStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock removed successfully", stock));
    }

    @Operation(summary = "Reserve stock quantity", description = "Reserves a quantity of a product at a branch (prevents checkouts of the reserved amount by other cashier transactions)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock reserved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient available stock to reserve requested amount")
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<?>> reserveStock(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        StockDTO stock = stockService.reserveStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock reserved successfully", stock));
    }

    @Operation(summary = "Release reserved stock", description = "Releases previously reserved stock back to available counts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reserved stock released successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Quantity to release exceeds currently reserved amount")
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<?>> releaseReservedStock(
            @RequestParam Long branchId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        StockDTO stock = stockService.releaseReservedStock(branchId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Reserved stock released successfully", stock));
    }

    @Operation(summary = "Get stock report", description = "Generates a summary report of stocks, inventory valuations, and item counts (with optional branch filter)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock report generated successfully")
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<?>> getStockReport(
            @RequestParam(required = false) Long branchId) {
        List<StockReportDTO> report = stockService.getStockReport(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stock report generated successfully", report));
    }

    @Operation(summary = "Get low stock alerts", description = "Lists product stocks that have dropped below their designated minimum threshold level")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Low stock list compiled successfully")
    @GetMapping("/alerts/low-stock")
    public ResponseEntity<ApiResponse<?>> getLowStockAlerts(
            @RequestParam(required = false) Long branchId) {
        List<LowStockAlertDTO> alerts = stockService.getLowStockAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success("Low stock alerts retrieved successfully", alerts));
    }
}

