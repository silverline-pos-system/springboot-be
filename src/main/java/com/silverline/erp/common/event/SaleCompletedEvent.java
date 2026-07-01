package com.silverline.erp.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class SaleCompletedEvent {
    private final Long saleId;
    private final String invoiceNo;
    private final Long branchId;
    private final Long cashierId;
    private final String cashierUsername;
    private final BigDecimal netTotal;
    private final int itemCount;
}
