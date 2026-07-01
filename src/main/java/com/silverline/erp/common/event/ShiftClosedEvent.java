package com.silverline.erp.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class ShiftClosedEvent {
    private final Long shiftId;
    private final String shiftNo;
    private final Long branchId;
    private final Long cashierId;
    private final String cashierUsername;
    private final BigDecimal closingCash;
    private final BigDecimal expectedCash;
    private final BigDecimal cashDifference;
    private final String notes;
}
