package com.nsbm.rocs.modules.pos.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesHistoryDTO {
    private String date;
    private BigDecimal quantitySold;
    private BigDecimal totalRevenue;

    public ProductSalesHistoryDTO(Object date, BigDecimal quantitySold, BigDecimal totalRevenue) {
        this.date = date != null ? date.toString() : null;
        this.quantitySold = quantitySold != null ? quantitySold : BigDecimal.ZERO;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }
}
