package com.silverline.erp.module.procurement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PurchaseOrderResponse {
    private Long poId;
    private String poNo;
    private Long branchId;
    private Long supplierId;
    private String supplierName;
    private LocalDate poDate;
    private LocalDate expectedDeliveryDate;
    private String paymentTerms;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
    private String status;
    private Long createdBy;
    private String requestedBy;
    private LocalDateTime createdAt;

    // Payment fields
    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference;
    private LocalDateTime paidDate;
    private BigDecimal paidAmount;
}


