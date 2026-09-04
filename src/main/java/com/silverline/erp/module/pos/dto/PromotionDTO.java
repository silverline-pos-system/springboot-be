package com.silverline.erp.module.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDTO {
    private Long promotionId;
    private String name;
    private String description;
    private String promoType;
    private Long branchId;
    private String scopeType;
    private Long scopeRefId;
    private BigDecimal buyQty;
    private BigDecimal getQty;
    private Long getProductId;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal fixedPrice;
    private BigDecimal minCartAmount;
    private Integer clearanceDays;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer priority;
    private Boolean stackable;
    private Boolean isActive;
    private Integer maxUses;
    private Integer usesCount;
}
