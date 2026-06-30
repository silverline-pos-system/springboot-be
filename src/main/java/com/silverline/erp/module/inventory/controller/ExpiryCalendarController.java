package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;

import com.silverline.erp.module.inventory.dto.ExpiryAlertDTO;
import com.silverline.erp.module.inventory.service.ExpiryCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/expiry-calendar")
@RequiredArgsConstructor
public class ExpiryCalendarController {

    private final ExpiryCalendarService expiryCalendarService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getExpiryCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long branchId) {
        List<ExpiryAlertDTO> expiryData = expiryCalendarService.getExpiryCalendar(start, end, branchId);
        return ResponseEntity.ok(ApiResponse.success("Expiry calendar retrieved successfully", expiryData));
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<ApiResponse<?>> getExpiringSoon(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "30") Integer daysAhead) {
        List<ExpiryAlertDTO> expiringData = expiryCalendarService.getExpiringSoon(branchId, daysAhead);
        return ResponseEntity.ok(ApiResponse.success("Expiring products retrieved successfully", expiringData));
    }

    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<?>> getExpired(
            @RequestParam(required = false) Long branchId) {
        List<ExpiryAlertDTO> expiredData = expiryCalendarService.getExpired(branchId);
        return ResponseEntity.ok(ApiResponse.success("Expired products retrieved successfully", expiredData));
    }
}



