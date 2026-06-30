package com.silverline.erp.module.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchAlertDTO {
    private Long alertId;
    private String message;
    private String time;
    private String type;
}


