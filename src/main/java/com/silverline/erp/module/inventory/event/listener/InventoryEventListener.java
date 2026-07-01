package com.silverline.erp.module.inventory.event.listener;

import com.silverline.erp.common.event.StockAdjustedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryEventListener {

    @Async
    @EventListener
    public void handleStockAdjusted(StockAdjustedEvent event) {
        log.info("Received StockAdjustedEvent asynchronously - Product ID: {}, Branch ID: {}, Qty: {}, Type: {} on thread {}",
                event.getProductId(), event.getBranchId(), event.getQuantity(), event.getAdjustmentType(), Thread.currentThread().getName());
    }
}
