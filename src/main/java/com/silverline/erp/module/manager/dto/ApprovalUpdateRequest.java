package com.silverline.erp.module.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalUpdateRequest {
    private String status;
    private String notes;
    private String role;
    private Long approverId;
}

