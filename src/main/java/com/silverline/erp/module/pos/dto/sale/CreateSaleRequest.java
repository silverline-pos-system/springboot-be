package com.silverline.erp.module.pos.dto.sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class CreateSaleRequest {

    private Long saleId;    // NEW: For updating Held/Pending sales
    private Long customerId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;  // NEW: Branch selected in POS (user not tied to branch)

    @NotEmpty(message = "Sale must contain at least one item")
    @Valid
    private List<SaleItemRequest> items;

    @Valid
    private List<PaymentRequest> payments;

    private BigDecimal discount;

    private String saleType;
    private String notes;
    private String status; // NEW FIELD for Hold/Pending

    // Optional client-generated key (e.g. a UUID per checkout attempt). If the same key is retried
    // after a network hiccup, the server returns the original sale instead of creating a duplicate.
    private String idempotencyKey;

    @Override
    public String toString() {
        return "CreateSaleRequest{" +
                "customerId=" + customerId +
                ", items=" + items +
                ", payments=" + payments +
                ", discount=" + discount +
                ", status='" + status + '\'' + // Add to toString
                ", notes='" + notes + '\'' +
                '}';
    }
}
