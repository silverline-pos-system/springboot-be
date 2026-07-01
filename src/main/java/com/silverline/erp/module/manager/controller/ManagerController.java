package com.silverline.erp.module.manager.controller;

import com.silverline.erp.common.dto.ApiResponse;
import com.silverline.erp.module.analytics.dto.StaffSummaryDTO;
import com.silverline.erp.module.manager.dto.ActivityLogDTO;
import com.silverline.erp.module.manager.dto.ApprovalDTO;
import com.silverline.erp.module.manager.dto.ApprovalUpdateRequest;
import com.silverline.erp.module.manager.service.ManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Manager Dashboard endpoints.
 * Base path: /api/v1/manager
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;

    // ===== STAFF SUMMARY =====

    @GetMapping("/staff/summary")
    public ResponseEntity<ApiResponse<List<StaffSummaryDTO>>> getStaffSummary(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching staff summary for branchId: {}", branchId);
        List<StaffSummaryDTO> staff = managerService.getStaffSummary(branchId);
        return ResponseEntity.ok(ApiResponse.success("Staff summary fetched successfully", staff));
    }

    // ===== APPROVALS =====

    @GetMapping("/approvals")
    public ResponseEntity<ApiResponse<List<ApprovalDTO>>> getApprovals(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching approvals with status: {}, branchId: {}", status, branchId);
        List<ApprovalDTO> approvals = managerService.getApprovals(status, branchId);
        return ResponseEntity.ok(ApiResponse.success("Approvals fetched successfully", approvals));
    }

    @GetMapping("/approvals/me")
    public ResponseEntity<ApiResponse<List<ApprovalDTO>>> getMyApprovals(
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching my approval requests for branchId: {}", branchId);
        List<ApprovalDTO> approvals = managerService.getMyApprovals(branchId);
        return ResponseEntity.ok(ApiResponse.success("Your approvals fetched successfully", approvals));
    }

    @PatchMapping("/approvals/{approvalId}")
    public ResponseEntity<ApiResponse<ApprovalDTO>> updateApprovalStatus(
            @PathVariable Long approvalId,
            @RequestBody ApprovalUpdateRequest request) {
        log.info("Updating approval {} to status: {}", approvalId, request.getStatus());
        ApprovalDTO approval = managerService.updateApprovalStatus(
                approvalId,
                request.getStatus(),
                request.getNotes(),
                request.getRole(),
                request.getApproverId()
        );
        return ResponseEntity.ok(ApiResponse.success("Approval status updated", approval));
    }

    // ===== BRANCH ACTIVITY LOG =====

    @GetMapping("/activity-log")
    public ResponseEntity<ApiResponse<List<ActivityLogDTO>>> getBranchActivityLog(
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-Branch-ID", required = false) Long branchId) {
        log.info("Fetching branch activity log with limit: {}, branchId: {}", limit, branchId);
        List<ActivityLogDTO> activities = managerService.getBranchActivityLog(limit, branchId);
        return ResponseEntity.ok(ApiResponse.success("Activity log fetched successfully", activities));
    }
}
