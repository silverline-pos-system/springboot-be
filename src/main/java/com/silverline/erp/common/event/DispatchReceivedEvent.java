package com.silverline.erp.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DispatchReceivedEvent {
    private final Long dispatchId;
    private final String dispatchNo;
    private final Long branchId;
    private final Long poId;
    private final Long approvedBy;
}
