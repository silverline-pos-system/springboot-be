package com.silverline.erp.module.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartOfAccountDTO {
    private String code;
    private String name;
    private String type;
    private String description;
}


