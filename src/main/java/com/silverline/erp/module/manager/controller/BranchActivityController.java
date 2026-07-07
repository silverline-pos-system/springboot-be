package com.silverline.erp.module.manager.controller;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.domain.audit.BranchActivity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/activity")
@RequiredArgsConstructor
@Tag(name = "Branch Activity Logs", description = "APIs for logging and auditing user/cashier operations within a specific branch")
public class BranchActivityController {

    private final AuditLogService activityLogService;

    @Operation(summary = "Get recent branch activities", description = "Retrieves the list of recent user activities logged for a branch")
    @ApiResponse(responseCode = "200", description = "Recent activity list fetched successfully")
    @GetMapping("/recent")
    public ResponseEntity<Object> getRecentActivity(@RequestParam(defaultValue = "1") Long branchId) {
        try {
            List<BranchActivity> activities = activityLogService.getRecentActivities(branchId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Recent activity fetched", "data", activities));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Get activities by date range", description = "Retrieves branch activities logged between start and end date-time ranges")
    @ApiResponse(responseCode = "200", description = "Filtered activities list fetched successfully")
    @GetMapping("/filter")
    public ResponseEntity<Object> getActivityByDate(
            @RequestParam(defaultValue = "1") Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        try {
            List<BranchActivity> activities = activityLogService.getActivitiesByDateRange(branchId, start, end);
            return ResponseEntity.ok(Map.of("success", true, "message", "Filtered activities fetched", "data", activities));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Log a branch activity manually", description = "Registers a new activity entry into the audit logs for a branch (e.g. cash drops, shifts updates)")
    @ApiResponse(responseCode = "200", description = "Activity logged successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payload or logging failure")
    @PostMapping("/log")
    public ResponseEntity<Object> logActivity(@RequestBody Map<String, Object> req) {
        try {
            Long branchId = req.get("branchId") != null ? Long.valueOf(req.get("branchId").toString()) : 1L;
            Long userId = req.get("userId") != null ? Long.valueOf(req.get("userId").toString()) : null;
            String username = (String) req.get("username");
            String role = (String) req.get("role");
            String actionType = (String) req.get("actionType");
            String details = (String) req.get("details");
            String metadata = (String) req.get("metadata");

            activityLogService.logActivity(branchId, userId, username, role, actionType, null, null, details, metadata);
            return ResponseEntity.ok(Map.of("success", true, "message", "Activity logged", "data", "OK"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Log failed: " + e.getMessage()));
        }
    }
}
