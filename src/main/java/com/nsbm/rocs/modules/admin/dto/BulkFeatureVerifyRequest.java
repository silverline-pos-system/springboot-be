package com.nsbm.rocs.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkFeatureVerifyRequest {
    private List<String> featureCodes;
    private String action; // ACTIVATE or DEACTIVATE
    private Long verificationKey;
}

