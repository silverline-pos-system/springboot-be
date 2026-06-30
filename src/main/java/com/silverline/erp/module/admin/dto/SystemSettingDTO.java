package com.silverline.erp.module.admin.dto;

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

