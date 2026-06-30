package com.nsbm.rocs.modules.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransactionDTO {
    private Long saleId;
    private String invoiceNo;
    private String cashier;
    private int itemCount;
    private BigDecimal amount;
    private String paymentMethod;
    private String type; // SALE, RETURN
    private String time;
    private String date;
}

