package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.StockDTO;
import com.silverline.erp.module.inventory.service.StockOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/stock-overview", "/api/inventory/stock-overview"})
@RequiredArgsConstructor
@Tag(name = "Stock Levels Overview", description = "APIs for querying real-time store inventory stocks and low stock metrics")
public class StockOverviewController {

    private final StockOverviewService stockOverviewService;

    @Operation(summary = "Get stock levels overview", description = "Retrieves stock counts and details, with optional filtering to a specific branch location")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock overview list retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getStockOverview(
            @RequestParam(required = false) Long branchId) {
        List<StockDTO> stockData = stockOverviewService.getStockOverview(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stock overview retrieved successfully", stockData));
    }

    @Operation(summary = "Get low stock products", description = "Retrieves items at a branch whose stock levels are at or below a specific threshold (defaults to 10)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Low stock items list retrieved successfully")
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<?>> getLowStockProducts(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<StockDTO> lowStock = stockOverviewService.getLowStockProducts(branchId, threshold);
        return ResponseEntity.ok(ApiResponse.success("Low stock products retrieved successfully", lowStock));
    }
}

