package com.silverline.erp.module.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignSecondaryRoleRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Secondary role is required")
    private String secondaryRole;       // "CASHIER", "SUPERVISOR", "STORE_KEEPER", "DTV_TECHNICIAN", "MOBILE_TECHNICIAN"

    @NotBlank(message = "Expiry timestamp is required")
    private String expiresAt;           // ISO 8601 string e.g. "2025-07-15T23:59:59.999Z"

    private String reason;

    @NotNull(message = "Assigned branch ID is required")
    private Long assignedByBranchId;
}


