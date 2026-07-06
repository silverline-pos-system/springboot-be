package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.LowStockAlertDTO;
import com.silverline.erp.module.inventory.dto.StockAdjustmentDTO;
import com.silverline.erp.module.inventory.dto.StockDTO;
import com.silverline.erp.module.inventory.dto.StockReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockService {
    Page<StockDTO> getAllStock(Pageable pageable);

    Page<StockDTO> getStockByBranch(Long branchId, Pageable pageable);

    List<StockDTO> getStockByProduct(Long productId);

    StockDTO getStockByBranchAndProduct(Long branchId, Long productId);

    StockDTO adjustStock(StockAdjustmentDTO adjustmentDTO);

    List<StockDTO> getLowStock(Long branchId);

    List<StockDTO> getOutOfStock(Long branchId);

    StockDTO addStock(Long branchId, Long productId, Integer quantity);

    StockDTO removeStock(Long branchId, Long productId, Integer quantity);

    StockDTO reserveStock(Long branchId, Long productId, Integer quantity);

    StockDTO releaseReservedStock(Long branchId, Long productId, Integer quantity);

    List<StockReportDTO> getStockReport(Long branchId);

    List<LowStockAlertDTO> getLowStockAlerts(Long branchId);

    // Cross-module APIs
    void reduceStock(Long branchId, Long productId, Integer quantity);

    void increaseStock(Long branchId, Long productId, Integer quantity);

    boolean isStockAvailable(Long branchId, Long productId, Integer quantity);

    Integer getCurrentStock(Long branchId, Long productId);
}
