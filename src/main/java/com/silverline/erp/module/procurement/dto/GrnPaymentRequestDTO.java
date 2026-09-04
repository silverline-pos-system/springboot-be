package com.silverline.erp.module.procurement.dto;

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
public class GrnPaymentRequestDTO {
    private Long requestId;
    private Long grnId;
    private String grnNo;
    private Long branchId;
    private String branchName;
    private Long supplierId;
    private String supplierName;
    private BigDecimal amount;
    private String invoiceNo;
    private String status;
    private String priority;
    private LocalDateTime dueDate;
    private String notes;
    private Long requestedBy;
    private String requestedByName;
    private Long supervisorApprovedBy;
    private String supervisorApprovedByName;
    private LocalDateTime supervisorApprovedAt;
    private Long transferredToManagerBy;
    private LocalDateTime transferredAt;
    private Long processedBy;
    private String processedByName;
    private LocalDateTime processedAt;
    private String paymentReference;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
