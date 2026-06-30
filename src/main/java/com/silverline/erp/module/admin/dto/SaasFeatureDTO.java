package com.silverline.erp.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaasFeatureDTO {

    private Long id;
    private String featureCode;
    private String featureName;
    private String featureCategory; // COMMON or PREMIUM
    private Boolean isActive;
    private String activatedAt;
    private String deactivatedAt;
    private String activatedByName;
}

