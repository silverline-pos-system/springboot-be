package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.service.ActivityLogService;
import com.silverline.erp.domain.audit.UserActivityLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/activity-logs")
public class ActivityLogController {

    private final ActivityLogService logService;

    @Autowired
    public ActivityLogController(ActivityLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ResponseEntity<List<UserActivityLog>> getActivityLogs(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return ResponseEntity.ok(logService.getLogsByFilter(branchId, type, startDate, endDate));
    }

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

