package com.silverline.erp.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureToggleRequest {
    private String featureCode;
    private String action; // ACTIVATE or DEACTIVATE
}

