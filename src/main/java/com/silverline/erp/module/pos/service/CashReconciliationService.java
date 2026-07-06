package com.silverline.erp.module.pos.service;

import com.silverline.erp.domain.pos.CashFlow;
import com.silverline.erp.module.pos.dto.CashFlowRequest;

import java.util.List;
import java.util.Map;

public interface CashReconciliationService {
    Map<String, Object> getShiftTotals(Long shiftId);

    CashFlow recordCashFlow(Long cashierId, CashFlowRequest request);

    List<CashFlow> getShiftCashFlows(Long shiftId);

    CashFlow findCashFlowById(Long id);

    CashFlow saveCashFlow(CashFlow cashFlow);
}
