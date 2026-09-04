package com.silverline.erp.module.procurement.dto;

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
public class GrnResponseDTO {

    private Long grnId;
    private String grnNo;
    private Long branchId;
    private String branchName;
    private Long supplierId;
    private String supplierName;
    private Long poId;
    private String poNo;
    private LocalDate grnDate;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    private BigDecimal netAmount;
    private String paymentStatus;
    private String status;
    private Long receivedBy;
    private String receivedByName;
    private Long postedBy;
    private String postedByName;
    private LocalDateTime createdAt;
    private List<GrnItemDTO> items;
}
