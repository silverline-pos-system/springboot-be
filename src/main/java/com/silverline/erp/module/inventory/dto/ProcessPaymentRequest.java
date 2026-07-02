package com.silverline.erp.module.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessPaymentRequest {
    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // BANK_TRANSFER, CHEQUE, CASH

    private String paymentReference;

    @PositiveOrZero(message = "Amount paid cannot be negative")
    private BigDecimal amountPaid;

    private String notes;
}

