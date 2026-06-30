package com.nsbm.rocs.modules.inventory.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProcessPORequest {
    private String status;
    private String notes;
    private String paymentMethod;
    private String paymentReference;
    private BigDecimal amountPaid;
    private LocalDateTime paidAt; // ISO format from JSON e.g. "2026-03-13T00:06:25.242Z"
}


