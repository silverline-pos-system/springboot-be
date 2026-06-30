package com.silverline.erp.module.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessPaymentRequest {
    private String paymentMethod; // BANK_TRANSFER, CHEQUE, CASH
    private String paymentReference;
    private BigDecimal amountPaid;
    private String notes;
}

