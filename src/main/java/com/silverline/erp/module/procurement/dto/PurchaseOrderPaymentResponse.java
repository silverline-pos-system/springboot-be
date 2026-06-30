package com.silverline.erp.module.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseOrderPaymentResponse {
    private Long poPaymentId;
    private Long poId;
    private String paymentMethod;
    private String paymentReference;
    private BigDecimal amountPaid;
    private LocalDateTime paidAt;
    private Long paidBy;
    private String notes;
}


