package com.nsbm.rocs.modules.inventory.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderDTO {
    private Long poId;
    private String poNo;
    private Long branchId;
    private Long supplierId;
    private LocalDate poDate;
    private LocalDate expectedDeliveryDate;
    private String paymentTerms;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
    private String status;
    private Long createdBy;
    private List<PurchaseOrderItemDTO> items;
}

