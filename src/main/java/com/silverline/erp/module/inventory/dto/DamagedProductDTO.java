package com.silverline.erp.module.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamagedProductDTO {
    private Long serialId;
    private Long productId;
    private String productName;
    private String serialNo;
    private Long branchId;
    private String branchName;
    private String reason;
    private LocalDateTime reportedAt;
    private Long reportedBy;
    private String productSku;
    private String batchCode;
    private String damageReason;
    private String message;
}


