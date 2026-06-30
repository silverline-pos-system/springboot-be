package com.nsbm.rocs.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleDTO {

    private Long saleId;
    private String invoiceNo;
    private Long branchId;
    private Long cashierId;
    private Long customerId;
    private Long shiftId;
    private LocalDateTime saleDate;

    // Money calculations
    private BigDecimal grossTotal;
    private BigDecimal discount;
    private BigDecimal taxAmount;
    private BigDecimal netTotal;
    private BigDecimal paidAmount;
    private BigDecimal changeAmount;

    // Status
    private String paymentStatus; // PAID, PARTIAL, PENDING
    private String saleType; // RETAIL, WHOLESALE

    private String notes;
    private LocalDateTime createdAt;
}

