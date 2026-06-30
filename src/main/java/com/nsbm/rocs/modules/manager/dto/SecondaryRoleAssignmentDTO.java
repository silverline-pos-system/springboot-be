package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecondaryRoleAssignmentDTO {
    private Long id;
    private Long userId;
    private String username;            // joined from users table
    private String primaryRole;         // joined from users table (user's original role)
    private String secondaryRole;
    private String expiresAt;           // ISO 8601 string
    private String reason;
    private Long assignedByBranchId;
    private String createdAt;           // ISO 8601 string
    private Boolean revoked;
}


