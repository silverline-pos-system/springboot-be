package com.silverline.erp.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Published after a GRN is posted (committed). Drives supplier payment request creation. */
@Getter
@RequiredArgsConstructor
public class GrnReceivedEvent {
    private final Long grnId;
    private final String grnNo;
    private final Long branchId;
    private final Long poId;
    private final Long postedBy;
}
