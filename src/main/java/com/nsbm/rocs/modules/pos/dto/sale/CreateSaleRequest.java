package com.nsbm.rocs.modules.pos.dto.sale;

import jakarta.validation.Valid;
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
    private Long branchId;  // NEW: Branch selected in POS (user not tied to branch)

    @Valid
    private List<SaleItemRequest> items;

    @Valid
    private List<PaymentRequest> payments;

    private BigDecimal discount;

    private String saleType;
    private String notes;
    private String status; // NEW FIELD for Hold/Pending

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
