package com.silverline.erp.module.manager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalUpdateRequest {
    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
    private String role;
}
