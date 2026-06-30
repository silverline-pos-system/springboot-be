package com.nsbm.rocs.modules.manager.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpensePaymentDTO {
    private Long paymentId;
    private Long expenseId;
    private String expenseNo;
    private LocalDate paymentDate;
    private String paymentMethod;
    private BigDecimal amount;
    private String referenceNo;
    private String notes;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}

