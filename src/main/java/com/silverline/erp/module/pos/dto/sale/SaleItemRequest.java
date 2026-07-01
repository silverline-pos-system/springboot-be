package com.silverline.erp.module.pos.dto.sale;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Setter
@Getter
@NoArgsConstructor
public class SaleItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long batchId;

    private Long serialId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    private BigDecimal discount;

    @Override
    public String toString() {
        return "SaleItemRequest{" +
                "productId=" + productId +
                ", serialId=" + serialId +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", discount=" + discount +
                '}';
    }
}
