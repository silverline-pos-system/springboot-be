package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/today-sales")
    public ResponseEntity<BigDecimal> getTodaysSales() {
        return ResponseEntity.ok(adminService.getTodaysSales());
    }

    @GetMapping("/user-stats")
    public ResponseEntity<Map<String, Long>> getUserStatsByRole() {
        return ResponseEntity.ok(adminService.getUserStatsByRole());
    }

    @GetMapping("/top-branches")
    public ResponseEntity<List<Map<String, Object>>> getTopBranches() {
        return ResponseEntity.ok(adminService.getTopBranches(5));
    }

    @GetMapping("/weekly-trend")
    public ResponseEntity<List<Map<String, Object>>> getWeeklySalesTrend() {
        return ResponseEntity.ok(adminService.getWeeklySalesTrend());
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        return ResponseEntity.ok(adminService.getDashboardOverview());
    }

    // Stub for now, can be implemented properly
    @GetMapping("/customer-recurrence")
    public ResponseEntity<List<Map<String, Object>>> getCustomerRecurrence() {
        return ResponseEntity.ok(List.of());
    }

    // Stub for now
    @GetMapping("/top-managers")
    public ResponseEntity<List<Map<String, Object>>> getTopManagers() {
        return ResponseEntity.ok(List.of());
    }
}

