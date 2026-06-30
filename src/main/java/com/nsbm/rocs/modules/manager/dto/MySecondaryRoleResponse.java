package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MySecondaryRoleResponse {
    private String secondaryRole;
    private String expiresAt;           // ISO 8601 string
    private String reason;
}


