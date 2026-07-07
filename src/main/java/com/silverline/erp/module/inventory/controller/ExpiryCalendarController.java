package com.silverline.erp.module.inventory.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.inventory.dto.ExpiryAlertDTO;
import com.silverline.erp.module.inventory.service.ExpiryCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/inventory/expiry-calendar", "/api/inventory/expiry-calendar"})
@RequiredArgsConstructor
@Tag(name = "Batch Expiry Tracking", description = "APIs for mapping batch expiry timelines, retrieving expiring soon lists, and auditing expired products")
public class ExpiryCalendarController {

    private final ExpiryCalendarService expiryCalendarService;

    @Operation(summary = "Get expiry calendar schedule", description = "Retrieves batch expiry logs scheduled between start and end dates, with optional branch filtering")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expiry calendar map fetched successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getExpiryCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long branchId) {
        List<ExpiryAlertDTO> expiryData = expiryCalendarService.getExpiryCalendar(start, end, branchId);
        return ResponseEntity.ok(ApiResponse.success("Expiry calendar retrieved successfully", expiryData));
    }

    @Operation(summary = "Get items expiring soon", description = "Lists batches expiring within a threshold number of days (default 30), with optional branch filtering")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expiring soon list retrieved successfully")
    @GetMapping("/expiring-soon")
    public ResponseEntity<ApiResponse<?>> getExpiringSoon(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "30") Integer daysAhead) {
        List<ExpiryAlertDTO> expiringData = expiryCalendarService.getExpiringSoon(branchId, daysAhead);
        return ResponseEntity.ok(ApiResponse.success("Expiring products retrieved successfully", expiringData));
    }

    @Operation(summary = "Get already-expired products", description = "Lists all registered batches whose expiration dates have passed, with optional branch filtering")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expired items list retrieved successfully")
    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<?>> getExpired(
            @RequestParam(required = false) Long branchId) {
        List<ExpiryAlertDTO> expiredData = expiryCalendarService.getExpired(branchId);
        return ResponseEntity.ok(ApiResponse.success("Expired products retrieved successfully", expiredData));
    }
}

