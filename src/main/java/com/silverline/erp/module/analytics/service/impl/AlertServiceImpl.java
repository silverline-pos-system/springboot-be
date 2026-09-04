package com.silverline.erp.module.analytics.service.impl;

import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.domain.procurement.Grn;
import com.silverline.erp.module.analytics.dto.BranchAlertDTO;
import com.silverline.erp.module.analytics.dto.ExpiryAlertDTO;
import com.silverline.erp.module.analytics.dto.StockAlertDTO;
import com.silverline.erp.module.analytics.service.AlertService;
import com.silverline.erp.module.inventory.dto.projection.ProductNameProjection;
import com.silverline.erp.module.inventory.dto.projection.ProductStockProjection;
import com.silverline.erp.module.inventory.repository.BatchRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.procurement.repository.GrnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final GrnRepository grnRepository;

    @Override
    public List<StockAlertDTO> getStockAlerts(Long branchId) {
        List<ProductStockProjection> products = productRepository.findActiveProjectionsByIsActiveTrue();
        List<StockAlertDTO> alerts = new ArrayList<>();

        List<Object[]> batchSums = batchRepository.sumQtyByProductIdAndBranchId(branchId);
        Map<Long, BigDecimal> qtyMap = batchSums.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
                        (a, b) -> a
                ));

        for (ProductStockProjection product : products) {
            BigDecimal totalQty = qtyMap.getOrDefault(product.getProductId(), BigDecimal.ZERO);
            BigDecimal reorderLevel = product.getReorderLevel() != null ? product.getReorderLevel() : BigDecimal.ZERO;

            if (totalQty.compareTo(reorderLevel) <= 0) {
                String level = totalQty.compareTo(BigDecimal.ZERO) == 0 ? "Critical" : "Low";

                alerts.add(StockAlertDTO.builder()
                        .productId(product.getProductId())
                        .item(product.getName())
                        .qty(totalQty)
                        .level(level)
                        .build());
            }
        }

        return alerts;
    }

    @Override
    public List<ExpiryAlertDTO> getExpiryAlerts(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);

        List<Batch> expiringBatches = batchRepository.findExpiringSoonBatchesByBranch(today, thirtyDaysFromNow, branchId);

        Map<Long, String> productNames = new HashMap<>();
        if (!expiringBatches.isEmpty()) {
            java.util.Set<Long> productIds = expiringBatches.stream()
                    .map(b -> b.getProductId())
                    .collect(Collectors.toSet());
            List<ProductNameProjection> nameProjections = productRepository.findByProductIdIn(productIds);
            productNames = nameProjections.stream()
                    .collect(Collectors.toMap(p -> p.getProductId(), p -> p.getName()));
        }

        final Map<Long, String> finalProductNames = productNames;
        return expiringBatches.stream()
                .map(batch -> {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(today, batch.getExpiryDate());
                    String severity = daysUntilExpiry <= 7 ? "Critical" : daysUntilExpiry <= 14 ? "Warning" : "Info";

                    return ExpiryAlertDTO.builder()
                            .batchId(batch.getBatchId())
                            .productId(batch.getProductId())
                            .item(finalProductNames.getOrDefault(batch.getProductId(), "Unknown"))
                            .expiresOn(batch.getExpiryDate().toString())
                            .qty(batch.getQty())
                            .severity(severity)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<BranchAlertDTO> getBranchAlerts(Long branchId) {
        List<BranchAlertDTO> alerts = new ArrayList<>();

        // Low stock alerts
        List<StockAlertDTO> stockAlerts = getStockAlerts(branchId);
        for (StockAlertDTO stockAlert : stockAlerts) {
            alerts.add(BranchAlertDTO.builder()
                    .alertId((long) alerts.size() + 1)
                    .message("Low stock: " + stockAlert.getItem() + " (" + stockAlert.getQty() + " left)")
                    .time("Now")
                    .type(stockAlert.getLevel())
                    .build());
        }

        // Expiry alerts
        List<ExpiryAlertDTO> expiryAlerts = getExpiryAlerts(branchId);
        for (ExpiryAlertDTO expiryAlert : expiryAlerts) {
            if ("Critical".equals(expiryAlert.getSeverity())) {
                alerts.add(BranchAlertDTO.builder()
                        .alertId((long) alerts.size() + 1)
                        .message("Expiring soon: " + expiryAlert.getItem() + " on " + expiryAlert.getExpiresOn())
                        .time("Now")
                        .type("Warning")
                        .build());
            }
        }

        // Pending GRN alerts (draft GRNs awaiting posting)
        List<Grn> pendingGrns;
        if (branchId != null) {
            pendingGrns = grnRepository.findByBranchIdAndStatus(branchId, "DRAFT");
        } else {
            pendingGrns = grnRepository.findByStatus("DRAFT");
        }

        if (!pendingGrns.isEmpty()) {
            alerts.add(BranchAlertDTO.builder()
                    .alertId((long) alerts.size() + 1)
                    .message(pendingGrns.size() + " draft GRN(s) awaiting posting")
                    .time("Now")
                    .type("Info")
                    .build());
        }

        return alerts;
    }
}
