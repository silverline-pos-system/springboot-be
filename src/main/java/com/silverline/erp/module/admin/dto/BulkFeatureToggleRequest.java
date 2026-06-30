package com.silverline.erp.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkFeatureToggleRequest {
    private List<String> featureCodes;
    private String action; // ACTIVATE or DEACTIVATE
}

