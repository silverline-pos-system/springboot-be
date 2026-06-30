package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyStatsDTO {
    private String title;
    private String value;
    private String description;
    private String trend; // up, down, neutral
    private String icon;  // icon key
}

