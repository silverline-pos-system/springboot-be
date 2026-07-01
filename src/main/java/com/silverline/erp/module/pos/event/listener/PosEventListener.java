package com.silverline.erp.module.pos.event.listener;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.common.event.SaleCompletedEvent;
import com.silverline.erp.common.event.ShiftClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PosEventListener {

    private final AuditLogService activityLogService;

    @Async
    @EventListener
    public void handleSaleCompleted(SaleCompletedEvent event) {
        log.info("Received SaleCompletedEvent for sale ID: {} on thread {}", event.getSaleId(), Thread.currentThread().getName());
        try {
            activityLogService.logActivity(
                    event.getBranchId(),
                    null,
                    event.getCashierId(),
                    event.getCashierUsername(),
                    "CASHIER",
                    "SALE",
                    "ORDER",
                    event.getSaleId(),
                    "Retail Sale " + event.getInvoiceNo() + ". Total: " + event.getNetTotal(),
                    "{\"itemCount\":" + event.getItemCount() + "}"
            );
        } catch (Exception e) {
            log.error("Failed to log activity for sale completed: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void handleShiftClosed(ShiftClosedEvent event) {
        log.info("Received ShiftClosedEvent for shift ID: {} on thread {}", event.getShiftId(), Thread.currentThread().getName());
        try {
            String closeMetadata = "{\"expected\":\"" + event.getExpectedCash() + "\",\"difference\":\"" + event.getCashDifference() + "\",\"closingCash\":\"" + event.getClosingCash() + "\"}";
            activityLogService.logActivity(
                    event.getBranchId(),
                    null,
                    event.getCashierId(),
                    event.getCashierUsername(),
                    "CASHIER",
                    "SHIFT_CLOSE",
                    "SHIFT",
                    event.getShiftId(),
                    "Shift #" + event.getShiftNo() + " closed. Closing Cash: " + event.getClosingCash(),
                    closeMetadata
            );
        } catch (Exception e) {
            log.error("Failed to log activity for shift closed: {}", e.getMessage(), e);
        }
    }
}
