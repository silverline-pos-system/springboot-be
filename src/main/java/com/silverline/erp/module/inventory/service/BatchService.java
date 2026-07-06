package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.BatchDTO;
import com.silverline.erp.module.inventory.dto.ExpiryAlertDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BatchService {
    Page<BatchDTO> getAllBatches(Pageable pageable);

    List<BatchDTO> getBatchesByProduct(Long productId);

    List<BatchDTO> getBatchesByBranch(Long branchId);

    BatchDTO getBatchById(Long id);

    BatchDTO getBatchByCode(Long branchId, Long productId, String batchCode);

    List<BatchDTO> getExpiredBatches();

    List<BatchDTO> getExpiringSoonBatches(int days);

    BatchDTO createBatch(BatchDTO batchDTO);

    BatchDTO updateBatch(Long id, BatchDTO batchDTO);

    void deleteBatch(Long id);

    List<ExpiryAlertDTO> getExpiryAlerts(int warningDays, int criticalDays);

    void deductByFEFO(Long branchId, Long productId, int qtyToDeduct);

    List<BatchDTO> getExpiredBatchesByBranchAndProduct(Long branchId, Long productId);

    List<BatchDTO> getExpiringSoonByBranchAndProduct(Long branchId, Long productId, int days);

    List<BatchDTO> getFEFOBatches(Long productId, Long branchId);

    void deductBatchStock(Long batchId, java.math.BigDecimal qty);
}
