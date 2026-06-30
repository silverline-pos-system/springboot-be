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
public class ManagerSaleDTO {
    private Long id;
    private String date;
    private BigDecimal amount;
    private String paymentStatus;
    private String invoiceNo;
}

