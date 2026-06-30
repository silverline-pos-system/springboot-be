package com.silverline.erp.module.inventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
public class StockTransferResponseDTO {

    // Getters and Setters
    private Long transferId;
    private String transferNo;
    private Long fromBranchId;
    private String fromBranchName;
    private Long toBranchId;
    private String toBranchName;
    private Long productId;
    private String productName;
    private Long batchId;
    private String batchCode;
    private Integer quantity;
    private LocalDate transferDate;
    private String remarks;
    private String transferStatus;
    private Long requestedBy;
    private String requestedByName;
    private LocalDateTime requestedTime;
    private Long approvedBy;
    private LocalDateTime approvedTime;

}

