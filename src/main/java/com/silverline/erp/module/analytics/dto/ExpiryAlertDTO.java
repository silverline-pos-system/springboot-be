package com.silverline.erp.module.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryAlertDTO {
    private Long batchId;
    private Long productId;
    private String item;
    private String expiresOn;
    private BigDecimal qty;
    private String severity;
}


