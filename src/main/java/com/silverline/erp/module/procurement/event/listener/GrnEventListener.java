package com.silverline.erp.module.procurement.event.listener;

import com.silverline.erp.common.event.GrnReceivedEvent;
import com.silverline.erp.module.procurement.service.GrnPaymentRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Creates the supplier payment request after a GRN is posted. Runs AFTER_COMMIT
 * so the GRN row is guaranteed committed and visible before the payment request
 * is created (fixes the race the old async dispatch listener had).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrnEventListener {

    private final GrnPaymentRequestService paymentRequestService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGrnReceived(GrnReceivedEvent event) {
        log.info("Handling GrnReceivedEvent for GRN {} on thread {}", event.getGrnNo(), Thread.currentThread().getName());
        try {
            paymentRequestService.createPaymentRequest(event.getGrnId(), event.getPostedBy());
            log.info("Created payment request for GRN {}", event.getGrnId());
        } catch (Exception e) {
            log.error("Failed to create payment request for GRN {}: {}", event.getGrnId(), e.getMessage(), e);
        }
    }
}
