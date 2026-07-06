package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.common.audit.repository.ApprovalRepository;
import com.silverline.erp.domain.audit.Approval;
import com.silverline.erp.domain.pos.CashFlow;
import com.silverline.erp.domain.pos.CashShift;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.pos.dto.CashFlowRequest;
import com.silverline.erp.module.pos.repository.*;
import com.silverline.erp.module.pos.service.CashReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CashReconciliationServiceImpl implements CashReconciliationService {

    private final ShiftRepository shiftRepository;
    private final CashFlowRepository cashFlowRepository;
    private final SaleRepository saleRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final PaymentRepository paymentRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditLogService activityLogService;
    private final UserProfileRepository userProfileRepository;

    @Override
    public Map<String, Object> getShiftTotals(Long shiftId) {
        CashShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        BigDecimal sales = saleRepository.sumNetTotalByShiftId(shiftId);
        if (sales == null) sales = BigDecimal.ZERO;

        BigDecimal returns = salesReturnRepository.sumTotalAmountByShiftId(shiftId);
        if (returns == null) returns = BigDecimal.ZERO;

        BigDecimal paidIn = cashFlowRepository.getTotalByTypeAndStatus(shiftId, "PAID_IN", "APPROVED");
        if (paidIn == null) paidIn = BigDecimal.ZERO;

        BigDecimal paidOut = cashFlowRepository.getTotalByTypeAndStatus(shiftId, "PAID_OUT", "APPROVED");
        if (paidOut == null) paidOut = BigDecimal.ZERO;

        BigDecimal pendingPaidIn = cashFlowRepository.getTotalByTypeAndStatus(shiftId, "PAID_IN", "PENDING");
        if (pendingPaidIn == null) pendingPaidIn = BigDecimal.ZERO;

        BigDecimal pendingPaidOut = cashFlowRepository.getTotalByTypeAndStatus(shiftId, "PAID_OUT", "PENDING");
        if (pendingPaidOut == null) pendingPaidOut = BigDecimal.ZERO;

        long pendingRequests = cashFlowRepository.countByShiftIdAndStatus(shiftId, "PENDING");

        BigDecimal cashSales = paymentRepository.sumTotalByShiftIdAndType(shiftId, "CASH");
        if (cashSales == null) cashSales = BigDecimal.ZERO;

        BigDecimal cardSales = paymentRepository.sumTotalByShiftIdAndType(shiftId, "CARD");
        if (cardSales == null) cardSales = BigDecimal.ZERO;

        BigDecimal otherSales = paymentRepository.sumOtherPaymentsByShiftId(shiftId);
        if (otherSales == null) otherSales = BigDecimal.ZERO;

        Map<String, Object> totals = new HashMap<>();
        totals.put("shiftId", shift.getShiftId());
        totals.put("openingCash", shift.getOpeningCash());
        totals.put("totalSales", sales);
        totals.put("cashSales", cashSales);
        totals.put("cardTotal", cardSales);
        totals.put("otherPayments", otherSales);
        totals.put("totalReturns", returns);
        totals.put("paidIn", paidIn);
        totals.put("paidOut", paidOut);
        totals.put("pendingPaidIn", pendingPaidIn);
        totals.put("pendingPaidOut", pendingPaidOut);
        totals.put("pendingRequests", pendingRequests);

        BigDecimal expected = shift.getOpeningCash()
                .add(cashSales)
                .subtract(returns)
                .add(paidIn)
                .subtract(paidOut);

        totals.put("expectedCash", expected);

        int transactionCount = saleRepository.countByShiftId(shiftId);
        totals.put("transactionCount", transactionCount);

        return totals;
    }

    @Override
    @Transactional
    public CashFlow recordCashFlow(Long cashierId, CashFlowRequest request) {
        CashShift shift;

        if (request.getShiftId() != null) {
            shift = shiftRepository.findById(request.getShiftId())
                    .orElseThrow(() -> new IllegalStateException("Shift not found: " + request.getShiftId()));

            if (shift.getStatus() != CashShift.ShiftStatus.OPEN) {
                throw new IllegalStateException("Cannot record cash flow for closed shift: " + request.getShiftId());
            }
        } else {
            shift = shiftRepository.findOpenShiftByCashierId(cashierId)
                    .orElseThrow(() -> new IllegalStateException("No open shift found for this cashier"));
        }

        CashFlow flow = new CashFlow();
        flow.setShiftId(shift.getShiftId());
        flow.setCreatedBy(cashierId);
        flow.setCreatedAt(LocalDateTime.now());
        flow.setType(request.getType());
        flow.setAmount(request.getAmount());
        flow.setReason(request.getReason());
        flow.setReferenceNo(request.getReferenceNo());
        flow.setStatus("PENDING");

        CashFlow savedFlow = cashFlowRepository.save(flow);

        if ("PENDING".equals(savedFlow.getStatus())) {
            Approval approval = new Approval();
            approval.setReferenceId(savedFlow.getFlowId());
            approval.setType("CASH_FLOW_" + flow.getType());
            approval.setStatus("PENDING");
            approval.setRequestedBy(cashierId);
            approval.setCreatedAt(LocalDateTime.now());
            approval.setBranchId(shift.getBranchId());

            approval.setReferenceNo("CF-" + savedFlow.getFlowId());
            approval.setRequestNotes(request.getType() + ": " + request.getAmount() + " - " + request.getReason());

            approvalRepository.save(approval);
        }

        String cashierUsername = userProfileRepository.findById(cashierId)
                .map(UserProfile::getUsername)
                .orElse("Cashier #" + cashierId);

        activityLogService.logActivity(
                shift.getBranchId(),
                null,
                cashierId,
                cashierUsername,
                "CASHIER",
                "CASH_FLOW_REQUEST",
                "CASH_FLOW",
                savedFlow.getFlowId(),
                "Requested " + request.getType() + " of " + request.getAmount(),
                "{\"reason\":\"" + (request.getReason() != null ? request.getReason().replace("\"", "'") : "") + "\",\"amount\":\"" + request.getAmount() + "\"}"
        );

        return savedFlow;
    }

    @Override
    public List<CashFlow> getShiftCashFlows(Long shiftId) {
        return cashFlowRepository.findByShiftId(shiftId);
    }

    @Override
    public CashFlow findCashFlowById(Long id) {
        return cashFlowRepository.findById(id).orElse(null);
    }

    @Override
    public CashFlow saveCashFlow(CashFlow cashFlow) {
        return cashFlowRepository.save(cashFlow);
    }
}
