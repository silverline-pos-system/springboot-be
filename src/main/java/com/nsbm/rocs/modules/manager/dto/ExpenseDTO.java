package com.nsbm.rocs.modules.manager.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExpenseDTO {
    private Long expenseId;
    private String expenseNo;
    private Long branchId;
    private String branchName;
    private Long categoryId;
    private String categoryName;
    private LocalDate expenseDate;
    private String description;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceNo;
    private String status;
    private Long createdBy;
    private String createdByName;
    private Long approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    
    // Derived fields for payments
    private BigDecimal totalPaid;
    private BigDecimal balance;
    
    private List<ExpensePaymentDTO> payments;
}

