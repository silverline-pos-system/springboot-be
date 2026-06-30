package com.nsbm.rocs.modules.admin.dto;

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

