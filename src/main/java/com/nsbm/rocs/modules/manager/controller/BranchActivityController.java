package com.nsbm.rocs.modules.manager.controller;

import com.nsbm.rocs.entity.audit.BranchActivity;
import com.nsbm.rocs.shared.audit.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/activity")
@CrossOrigin
public class BranchActivityController {

    @Autowired
    private AuditLogService activityLogService;

    @GetMapping("/recent")
    public ResponseEntity<Object> getRecentActivity(@RequestParam(defaultValue = "1") Long branchId) {
        try {
            List<BranchActivity> activities = activityLogService.getRecentActivities(branchId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Recent activity fetched", "data", activities));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

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

    @PostMapping("/log")
    public ResponseEntity<Object> logActivity(@RequestBody java.util.Map<String, Object> req) {
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


