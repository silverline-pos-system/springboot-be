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

    /**
     * The batch a sale line should be priced from and deducted first: the explicitly
     * chosen batch if it is valid and has stock, otherwise the FEFO (first-expired)
     * batch. Empty when the product has no batches at this branch.
     */
    java.util.Optional<com.silverline.erp.domain.inventory.Batch> resolveSaleBatch(Long branchId, Long productId, Long explicitBatchId);

    /**
     * Deduct a sold quantity for a product: the preferred (chosen/FEFO) batch first,
     * then the remainder oldest-first across the other batches. Best-effort at batch
     * level; the aggregate Stock row remains the oversell guard.
     */
    void deductForSale(Long branchId, Long productId, Long preferredBatchId, java.math.BigDecimal qty);
}
