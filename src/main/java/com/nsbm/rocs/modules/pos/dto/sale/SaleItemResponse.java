package com.nsbm.rocs.modules.pos.dto.sale;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
public class SaleItemResponse {

    private Long saleItemId;
    private Long productId;
    private Long batchId;
    private String productName;
    private String sku;
    private String barcode;
    private Long serialId;
    private String serialNo;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal total;
}
