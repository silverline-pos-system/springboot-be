package com.silverline.erp.module.manager.service;

import com.silverline.erp.module.analytics.dto.StaffSummaryDTO;
import com.silverline.erp.module.manager.dto.ActivityLogDTO;
import com.silverline.erp.module.manager.dto.ApprovalDTO;

import java.util.List;

public interface ManagerService {
    List<StaffSummaryDTO> getStaffSummary(Long branchId);

    List<ApprovalDTO> getMyApprovals(Long branchId);

    List<ApprovalDTO> getApprovals(String status, Long branchId);

    ApprovalDTO updateApprovalStatus(Long approvalId, String status, String notes, String role);

    List<ActivityLogDTO> getBranchActivityLog(int limit, Long branchId);
}
