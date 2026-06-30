package com.nsbm.rocs.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingDTO {
    private String settingKey;
    private String settingValue;
}

