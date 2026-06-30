package com.silverline.erp.module.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDTO {
    private Long activityId;
    private String time;
    private String user;
    private String action;
    private String details;
    private String severity;

    // Additional fields for frontend BranchActivityLog component
    private String actionType;      // e.g., SALE, LOGIN, SHIFT_OPEN
    private String username;        // user full name
    private String description;     // activity description
    private String createdAt;       // ISO timestamp for frontend date parsing
    private String status;          // SUCCESS, FAILED, etc.
    private Long branchId;          // branch ID
    private Long terminalId;        // terminal ID (if applicable)
    private Long userId;            // user ID
}

