package com.nsbm.rocs.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemResponse {
    private Long poItemId;
    private Long poId;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal qtyOrdered;
    private BigDecimal qtyDispatched;
    private BigDecimal unitPrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private BigDecimal discount;
    private BigDecimal total;
}
