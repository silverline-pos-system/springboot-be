package com.nsbm.rocs.modules.pos.dto.returns;

import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class ReturnRequest {
    private Long saleId;
    private Long branchId;
    private String reason;
    private String refundMethod;
    private List<ReturnItemRequest> items;
    private String supervisorUsername;
    private String supervisorPassword;

    @Data
    public static class ReturnItemRequest {
        private Long saleItemId;
        private Long productId;
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private String condition;
    }
}

