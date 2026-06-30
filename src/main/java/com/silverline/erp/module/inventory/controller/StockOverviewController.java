package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.common.dto.ApiResponse;

import com.silverline.erp.module.inventory.dto.StockDTO;
import com.silverline.erp.module.inventory.service.StockOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/stock-overview")
@RequiredArgsConstructor
public class StockOverviewController {

    private final StockOverviewService stockOverviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getStockOverview(
            @RequestParam(required = false) Long branchId) {
        List<StockDTO> stockData = stockOverviewService.getStockOverview(branchId);
        return ResponseEntity.ok(ApiResponse.success("Stock overview retrieved successfully", stockData));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<?>> getLowStockProducts(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<StockDTO> lowStock = stockOverviewService.getLowStockProducts(branchId, threshold);
        return ResponseEntity.ok(ApiResponse.success("Low stock products retrieved successfully", lowStock));
    }
}



