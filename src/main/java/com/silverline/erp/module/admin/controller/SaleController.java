package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.service.AdminSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/admin/sales")
@RequiredArgsConstructor
@Tag(name = "Sales Override & Status Sync", description = "APIs for administrators and managers to query global sales totals, branch-specific net totals, and historical financial aggregates")
public class SaleController {

    private final AdminSaleService saleService;

    @Operation(summary = "Get sum of sales net total", description = "Queries total net sales value. Can filter results to a specific branch location and narrow by startDate/endDate ISO-8601 timestamps.")
    @ApiResponse(responseCode = "200", description = "Sales net total sum calculated successfully")
    @GetMapping("/sum")
    public BigDecimal getSumNetTotal(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;

        try {
            if (startDate != null && !startDate.trim().isEmpty()) {
                start = LocalDateTime.parse(startDate, fmt);
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                end = LocalDateTime.parse(endDate, fmt);
            }
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }

        return saleService.getSumNetTotal(branchId, start, end);
    }

    @Operation(summary = "Get all-time sales sum", description = "Retrieves the sum total of net sales recorded across all branches for all time")
    @ApiResponse(responseCode = "200", description = "All-time sales total retrieved successfully")
    @GetMapping("/sum/all-time")
    public BigDecimal getSumNetTotalAllTime() {
        return saleService.getTotalNetAllTime();
    }
}
