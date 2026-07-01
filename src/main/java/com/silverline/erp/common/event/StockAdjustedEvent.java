package com.silverline.erp.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class StockAdjustedEvent {
    private final Long productId;
    private final Long branchId;
    private final BigDecimal quantity;
    private final String adjustmentType;
}
