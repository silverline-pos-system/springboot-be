package com.silverline.erp.module.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PurchaseOrderItemDTO {
    private Long poItemId;
    private Long productId;
    private BigDecimal qtyOrdered;
    private BigDecimal unitPrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private BigDecimal discount;
    private BigDecimal total;
}

