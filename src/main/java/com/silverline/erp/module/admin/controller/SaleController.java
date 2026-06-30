package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/admin/sales")
public class SaleController {

    private final SaleService saleService;

    @Autowired
    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    /**
     * GET /api/v1/admin/sales/sum
     * Optional query params:
     *   branchId - Long
     *   startDate - ISO-8601 LocalDateTime string (e.g. 2026-02-01T00:00:00)
     *   endDate   - ISO-8601 LocalDateTime string
     *
     * Returns the sum of netTotal as a JSON number.
     */
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
            // For simplicity return zero on parse error; you may replace with BadRequest handling.
            return BigDecimal.ZERO;
        }

        return saleService.getSumNetTotal(branchId, start, end);
    }

    /**
         * GET /api/v1/admin/sales/sum/all-time
     * Returns the total sum of netTotal across all branches for all time.
     */
    @GetMapping("/sum/all-time")
    public BigDecimal getSumNetTotalAllTime() {
        return saleService.getTotalNetAllTime();
    }
}

