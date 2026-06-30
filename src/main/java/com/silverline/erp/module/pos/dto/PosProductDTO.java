package com.silverline.erp.module.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PosProductDTO {
    private Long productId;
    private String name;
    private BigDecimal sellingPrice;
    private String sku;
    private String barcode;
    private String categoryName;
    private Boolean isSerialized;
    private BigDecimal availableStock;
    private java.util.List<BatchPriceDTO> availablePrices;
    private Long selectedSerialId;
    private String serialNo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchPriceDTO {
        private Long batchId;
        private String batchCode;
        private BigDecimal sellingPrice;
        private BigDecimal mrp;
        private BigDecimal stockQty;
        private java.time.LocalDate expiryDate;
    }
}


