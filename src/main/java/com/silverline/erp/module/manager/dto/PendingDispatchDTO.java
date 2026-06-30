package com.silverline.erp.module.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingDispatchDTO {
    private String id;
    private String supplier;
    private Integer items;
    private String eta;
    private String requestedBy;
}

