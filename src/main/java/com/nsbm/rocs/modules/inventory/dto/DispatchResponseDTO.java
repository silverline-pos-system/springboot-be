package com.nsbm.rocs.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispatchResponseDTO {

    private Long dispatchId;
    private String dispatchNo;
    private Long branchId;
    private String branchName;
    private Long supplierId;
    private String supplierName;
    private Long poId;
    private String poNo;
    private LocalDate dispatchDate;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    private BigDecimal netAmount;
    private String paymentStatus;
    private String status;
    private Long createdBy;
    private String createdByName;
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime createdAt;
    private List<DispatchItemDTO> items;
}

