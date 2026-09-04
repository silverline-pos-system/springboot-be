package com.silverline.erp.module.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Input lines and output of a promotion evaluation. Kept in one file for cohesion. */
public class PromotionEval {

    /** A cart line to evaluate (already priced from its batch/branch). */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private Long productId;
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private LocalDate batchExpiry; // for expiry-clearance; nullable
    }

    /** Extra discount applied to a cart line by a promotion. */
    @Data
    @AllArgsConstructor
    public static class LineDiscount {
        private int lineIndex;
        private BigDecimal discount;
        private Long promotionId;
        private String reason;
    }

    /** A free giveaway line added by a promotion. */
    @Data
    @AllArgsConstructor
    public static class FreeItem {
        private Long productId;
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private Long promotionId;
        private String reason;
    }

    /** Summary of a promotion that fired. */
    @Data
    @AllArgsConstructor
    public static class Applied {
        private Long promotionId;
        private String name;
        private String promoType;
        private BigDecimal benefit;
    }

    /** Outcome of evaluating all active promotions against a cart. */
    @Data
    public static class Outcome {
        private List<LineDiscount> lineDiscounts = new ArrayList<>();
        private List<FreeItem> freeItems = new ArrayList<>();
        private List<Applied> applied = new ArrayList<>();
        private BigDecimal cartDiscount = BigDecimal.ZERO;
    }
}
