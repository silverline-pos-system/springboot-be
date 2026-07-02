package com.silverline.erp.module.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProcessPORequest {
    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
    private String paymentMethod;
    private String paymentReference;

    @PositiveOrZero(message = "Amount paid cannot be negative")
    private BigDecimal amountPaid;

    private LocalDateTime paidAt; // ISO format from JSON e.g. "2026-03-13T00:06:25.242Z"
}


