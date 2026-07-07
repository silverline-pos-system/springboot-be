package com.silverline.erp.module.pos.controller;

import com.silverline.erp.module.pos.dto.sale.ProductSalesHistoryDTO;
import com.silverline.erp.module.pos.service.SaleQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/sales", "/api/sales"})
@RequiredArgsConstructor
@Tag(name = "POS Historical Invoices", description = "APIs for cashiers to retrieve historical checkout summaries and product sales metrics")
public class SalesController {

    private final SaleQueryService saleQueryService;

    @Operation(summary = "Get product sales history", description = "Retrieves a historical list of checkout sale transactions for a specific product ID between date ranges (defaults to the last 30 days)")
    @ApiResponse(responseCode = "200", description = "Sales history list retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ProductSalesHistoryDTO>> getProductSalesHistory(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        List<ProductSalesHistoryDTO> history = saleQueryService.getProductSalesHistory(productId, from, to);
        return ResponseEntity.ok(history);
    }
}
