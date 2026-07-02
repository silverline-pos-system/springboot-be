package com.silverline.erp.module.analytics.service.impl;

import com.silverline.erp.domain.procurement.Dispatch;
import com.silverline.erp.module.analytics.dto.DashboardStatsDTO;
import com.silverline.erp.module.analytics.dto.StockAlertDTO;
import com.silverline.erp.module.analytics.service.AlertService;
import com.silverline.erp.module.inventory.repository.SupplierRepository;
import com.silverline.erp.module.manager.dto.PendingDispatchDTO;
import com.silverline.erp.module.manager.repository.ManagerSaleRepository;
import com.silverline.erp.module.manager.repository.ManagerUserRepository;
import com.silverline.erp.module.procurement.repository.DispatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ManagerSaleRepository saleRepository;
    @Mock
    private DispatchRepository dispatchRepository;
    @Mock
    private ManagerUserRepository userRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private AlertService alertService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getDashboardStats_ForBranch() {
        // Arrange
        when(saleRepository.sumNetTotalByBranchAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(1000)) // today
                .thenReturn(BigDecimal.valueOf(800));  // yesterday
        when(saleRepository.countByBranchAndDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);

        Dispatch mockDispatch = new Dispatch();
        mockDispatch.setDispatchId(50L);
        when(dispatchRepository.findByBranchIdAndStatus(1L, "PENDING"))
                .thenReturn(Collections.singletonList(mockDispatch));

        StockAlertDTO mockAlert = new StockAlertDTO();
        when(alertService.getStockAlerts(1L)).thenReturn(Collections.singletonList(mockAlert));

        // Act
        List<DashboardStatsDTO> stats = dashboardService.getDashboardStats(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(4, stats.size());
        assertEquals("Today's Sales", stats.get(0).getTitle());
        assertEquals("Transactions", stats.get(1).getTitle());
        assertEquals("pending dispatches", stats.get(2).getTitle());
        assertEquals("1", stats.get(2).getValue());
        assertEquals("Low Stock Items", stats.get(3).getTitle());
        assertEquals("1", stats.get(3).getValue());
    }

    @Test
    void getPendingDispatches_Success() {
        // Arrange
        Dispatch mockDispatch = new Dispatch();
        mockDispatch.setDispatchId(50L);
        mockDispatch.setSupplierId(10L);
        mockDispatch.setDispatchNo("INV-999");
        mockDispatch.setStatus("PENDING");

        when(dispatchRepository.findByBranchIdAndStatus(1L, "PENDING"))
                .thenReturn(Collections.singletonList(mockDispatch));
        when(supplierRepository.findById(10L)).thenReturn(Optional.empty());

        // Act
        List<PendingDispatchDTO> list = dashboardService.getPendingDispatches(1L);

        // Assert
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("INV-999", list.getFirst().getId());
    }
}
