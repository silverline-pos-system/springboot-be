package com.silverline.erp.module.analytics.service.impl;

import com.silverline.erp.domain.procurement.Dispatch;
import com.silverline.erp.domain.procurement.Supplier;
import com.silverline.erp.module.analytics.dto.DashboardStatsDTO;
import com.silverline.erp.module.analytics.dto.StockAlertDTO;
import com.silverline.erp.module.analytics.service.AlertService;
import com.silverline.erp.module.analytics.service.DashboardService;
import com.silverline.erp.module.inventory.repository.SupplierRepository;
import com.silverline.erp.module.manager.dto.PendingDispatchDTO;
import com.silverline.erp.module.manager.repository.ManagerSaleRepository;
import com.silverline.erp.module.manager.repository.ManagerUserRepository;
import com.silverline.erp.module.procurement.repository.DispatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ManagerSaleRepository saleRepository;
    private final DispatchRepository dispatchRepository;
    private final ManagerUserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final AlertService alertService;

    @Override
    public List<DashboardStatsDTO> getDashboardStats(Long branchId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);

        BigDecimal todaySales;
        Long todayTransactions;
        BigDecimal yesterdaySales;
        List<Dispatch> pendingDispatches;

        if (branchId != null) {
            todaySales = saleRepository.sumNetTotalByBranchAndDateRange(branchId, todayStart, todayEnd);
            todayTransactions = saleRepository.countByBranchAndDateRange(branchId, todayStart, todayEnd);
            yesterdaySales = saleRepository.sumNetTotalByBranchAndDateRange(branchId, yesterdayStart, yesterdayEnd);
            pendingDispatches = dispatchRepository.findByBranchIdAndStatus(branchId, "PENDING");
        } else {
            todaySales = saleRepository.sumNetTotalByDateRange(todayStart, todayEnd);
            todayTransactions = saleRepository.countByDateRange(todayStart, todayEnd);
            yesterdaySales = saleRepository.sumNetTotalByDateRange(yesterdayStart, yesterdayEnd);
            pendingDispatches = dispatchRepository.findByStatus("PENDING");
        }

        todaySales = todaySales != null ? todaySales : BigDecimal.ZERO;
        yesterdaySales = yesterdaySales != null ? yesterdaySales : BigDecimal.ZERO;

        List<StockAlertDTO> lowStockAlerts = alertService.getStockAlerts(branchId);

        List<DashboardStatsDTO> stats = new ArrayList<>();

        stats.add(DashboardStatsDTO.builder()
                .title("Today's Sales")
                .value(formatCurrency(todaySales))
                .icon("currency")
                .tone(todaySales.compareTo(yesterdaySales) >= 0 ? "success" : "warning")
                .build());

        stats.add(DashboardStatsDTO.builder()
                .title("Transactions")
                .value(String.valueOf(todayTransactions))
                .icon("receipt")
                .tone("info")
                .build());

        stats.add(DashboardStatsDTO.builder()
                .title("pending dispatches")
                .value(String.valueOf(pendingDispatches.size()))
                .icon("truck")
                .tone(pendingDispatches.isEmpty() ? "success" : "warning")
                .build());

        stats.add(DashboardStatsDTO.builder()
                .title("Low Stock Items")
                .value(String.valueOf(lowStockAlerts.size()))
                .icon("package")
                .tone(lowStockAlerts.isEmpty() ? "success" : "danger")
                .build());

        return stats;
    }

    @Override
    public List<PendingDispatchDTO> getPendingDispatches(Long branchId) {
        List<Dispatch> pendingDispatches;
        if (branchId != null) {
            pendingDispatches = dispatchRepository.findByBranchIdAndStatus(branchId, "PENDING");
        } else {
            pendingDispatches = dispatchRepository.findByStatus("PENDING");
        }

        return pendingDispatches.stream()
                .map(dispatch -> {
                    String supplierName = getSupplierName(dispatch.getSupplierId());
                    String eta = calculateEta(dispatch.getDispatchDate());

                    return PendingDispatchDTO.builder()
                            .id(dispatch.getDispatchNo())
                            .supplier(supplierName)
                            .items(0)
                            .eta(eta)
                            .requestedBy(dispatch.getCreatedBy() != null ?
                                    userRepository.findById(dispatch.getCreatedBy())
                                            .map(u -> u.getFullName())
                                            .orElse("ID: " + dispatch.getCreatedBy()) : "System")
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return "LKR " + String.format("%,.0f", amount);
    }

    private String getSupplierName(Long supplierId) {
        if (supplierId == null) return "Unknown";
        return supplierRepository.findById(supplierId)
                .map(s -> s.getName())
                .orElse("Unknown Supplier");
    }

    private String calculateEta(LocalDate dispatchDate) {
        if (dispatchDate == null) return "Unknown";
        LocalDate today = LocalDate.now();
        if (dispatchDate.equals(today)) return "Today";
        if (dispatchDate.equals(today.plusDays(1))) return "Tomorrow";
        if (dispatchDate.isBefore(today)) return "Overdue";
        return dispatchDate.toString();
    }
}
