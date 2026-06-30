package com.silverline.erp.module.pos.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleSummaryDTO {
    private Long saleId;      // Primary Key
    private String id;        // Mapped from invoiceNo
    private String time;      // Formatted saleDate
    private Integer items;    // Count of items
    private BigDecimal netTotal; // Was total
    private BigDecimal grossTotal;
    private String customerName;
    private String status;    // paymentStatus
}


