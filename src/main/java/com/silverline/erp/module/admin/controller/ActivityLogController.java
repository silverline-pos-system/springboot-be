package com.silverline.erp.module.admin.controller;

import com.silverline.erp.domain.audit.UserActivityLog;
import com.silverline.erp.module.admin.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/activity-logs")
@Tag(name = "Audit Logs & Activities", description = "APIs for administrators to view, search, and filter transaction, login, and system override activity logs")
public class ActivityLogController {

    private final ActivityLogService logService;

    @Autowired
    public ActivityLogController(ActivityLogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "Get filtered activity logs", description = "Retrieves user action logs. Filters can restrict results to specific branches, action categories, or datetime ranges.")
    @ApiResponse(responseCode = "200", description = "Activity logs list retrieved successfully")
    @GetMapping
    public ResponseEntity<List<UserActivityLog>> getActivityLogs(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return ResponseEntity.ok(logService.getLogsByFilter(branchId, type, startDate, endDate));
    }

    @Operation(summary = "Search activity logs by keyword", description = "Searches logs matching keyword query within username, description details, or action values, with additional filters.")
    @ApiResponse(responseCode = "200", description = "Activity logs search completed successfully")
    @GetMapping("/search")
    public ResponseEntity<List<UserActivityLog>> searchActivityLogs(
            @RequestParam("q") String query,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return ResponseEntity.ok(logService.searchLogs(query, branchId, type, startDate, endDate));
    }
}
