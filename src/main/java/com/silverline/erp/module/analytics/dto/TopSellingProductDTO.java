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
public class TopSellingProductDTO {
    private Long productId;
    private String name;
    private String sku;
    private Integer units;
    private String revenue;
}


