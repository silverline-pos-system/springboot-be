package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSummaryDTO {
    private Long userId;
    private String name;
    private String role;
    private String lastLogin;
    private String status;
    private String approvedBy;
    private String email;
    private String phone;
    private String employeeId;
}


