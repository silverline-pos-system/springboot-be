package com.silverline.erp.module.procurement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispatchItemDTO {

    private Long dispatchItemId;
    private Long dispatchId;
    private Long productId;
    private String productName;
    private String productSku;
    private String batchCode;
    private LocalDate expiryDate;
    private BigDecimal qtyDispatched;
    private BigDecimal unitPrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private BigDecimal total;
}

