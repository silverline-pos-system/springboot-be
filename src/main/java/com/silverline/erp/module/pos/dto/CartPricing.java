package com.silverline.erp.module.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Request/response for the live cart pricing endpoint (batch prices + promotions). */
public class CartPricing {

    @Data
    @NoArgsConstructor
    public static class Request {
        private Long branchId;
        private List<Item> items = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long productId;
        private Long batchId;
        private BigDecimal qty;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricedLine {
        private Long productId;
        private String name;
        private Long batchId;
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private BigDecimal lineDiscount;
        private BigDecimal lineTotal;
        private boolean free;
        private Long promotionId;
        private String promotionName;
    }

    @Data
    @NoArgsConstructor
    public static class Response {
        private List<PricedLine> items = new ArrayList<>();
        private List<PromotionEval.Applied> promotions = new ArrayList<>();
        private BigDecimal subTotal = BigDecimal.ZERO;
        private BigDecimal discountTotal = BigDecimal.ZERO;
        private BigDecimal netTotal = BigDecimal.ZERO;
    }
}
