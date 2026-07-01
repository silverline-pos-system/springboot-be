package com.silverline.erp.module.procurement.event.listener;

import com.silverline.erp.common.event.DispatchReceivedEvent;
import com.silverline.erp.module.procurement.service.DispatchPaymentRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcurementEventListener {

    private final DispatchPaymentRequestService paymentRequestService;

    @Async
    @EventListener
    public void handleDispatchReceived(DispatchReceivedEvent event) {
        log.info("Received DispatchReceivedEvent asynchronously for dispatch: {} on thread {}", event.getDispatchNo(), Thread.currentThread().getName());
        try {
            paymentRequestService.createPaymentRequest(event.getDispatchId(), event.getApprovedBy());
            log.info("Asynchronously created payment request for dispatch: {}", event.getDispatchId());
        } catch (Exception e) {
            log.error("Failed to create payment request asynchronously for dispatch {}: {}", event.getDispatchId(), e.getMessage(), e);
        }
    }
}
