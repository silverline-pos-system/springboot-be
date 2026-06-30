package com.silverline.erp.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {

    private Long branchId;

    private String name;
    private String code;
    private String address;
    private String location;
    private String phone;
    private String email;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Input field: manager to assign to this branch
    private Long managerId;

    // Computed fields for frontend display
    private String managerName;
    private Integer terminalCount;
    
    // Additional computed fields for real metrics
    private Double dailySales;
    private Integer activeTerminals;
    private Integer totalTerminals;
    private Integer userCount;
    private Integer totalUsers;
    private Integer registeredCustomers;
}

