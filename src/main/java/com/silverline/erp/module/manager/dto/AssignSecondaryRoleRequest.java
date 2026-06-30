package com.silverline.erp.module.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignSecondaryRoleRequest {
    private Long userId;
    private String secondaryRole;       // "CASHIER", "SUPERVISOR", "STORE_KEEPER", "DTV_TECHNICIAN", "MOBILE_TECHNICIAN"
    private String expiresAt;           // ISO 8601 string e.g. "2025-07-15T23:59:59.999Z"
    private String reason;
    private Long assignedByBranchId;
}


