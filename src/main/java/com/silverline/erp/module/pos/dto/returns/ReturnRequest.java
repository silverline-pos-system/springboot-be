package com.silverline.erp.module.pos.dto.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnRequest {
    @NotNull(message = "Sale ID is required")
    private Long saleId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotBlank(message = "Return reason is required")
    private String reason;

    private String refundMethod;

    @NotEmpty(message = "Return must contain at least one item")
    @Valid
    private List<ReturnItemRequest> items;

    private String supervisorUsername;
    private String supervisorPassword;

    @Data
    public static class ReturnItemRequest {
        @NotNull(message = "Sale Item ID is required")
        private Long saleItemId;

        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private BigDecimal qty;

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price cannot be negative")
        private BigDecimal unitPrice;

        private String condition;
    }
}

