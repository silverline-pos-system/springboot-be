package com.silverline.erp.module.admin.controller;

import com.silverline.erp.module.admin.dto.DispatchDTO;
import com.silverline.erp.module.admin.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("adminDispatchController")
@RequestMapping("/api/v1/admin/dispatches")
@Tag(name = "Dispatch Log Audits", description = "APIs for administrators and managers to audit and view active dispatches across all retail branch locations")
public class DispatchController {

    private final DispatchService dispatchService;

    @Autowired
    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Operation(summary = "Get all pending dispatches", description = "Retrieves a comprehensive list of all pending dispatches across all store branches")
    @ApiResponse(responseCode = "200", description = "Dispatches list retrieved successfully")
    @GetMapping("/pending")
    public ResponseEntity<List<DispatchDTO>> getAllPendingDispatches() {
        List<DispatchDTO> list = dispatchService.getAllPendingDispatches();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get pending dispatch count", description = "Retrieves the count of pending dispatches. If a branch ID is supplied, filters the count to that branch location.")
    @ApiResponse(responseCode = "200", description = "Pending count retrieved successfully")
    @GetMapping("/count/pending")
    public ResponseEntity<Long> getPendingDispatchCount(@RequestParam(value = "branchId", required = false) Long branchId) {
        Long count = (branchId == null) ? dispatchService.getPendingDispatchCountAll() : dispatchService.getPendingDispatchCount(branchId);
        return ResponseEntity.ok(count);
    }
}
