package com.nsbm.rocs.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureVerifyRequest {
    private String featureCode;
    private String action;          // ACTIVATE or DEACTIVATE
    private Long verificationKey;   // The verification key entered by admin
}

