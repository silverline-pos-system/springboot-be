package com.nsbm.rocs.modules.inventory.service;

import com.nsbm.rocs.modules.inventory.dto.StockAdjustmentDTO;

import java.util.List;

public interface AdjustmentService {

    List<StockAdjustmentDTO> getAllAdjustments(Long branchId, Long productId);

    StockAdjustmentDTO createAdjustment(StockAdjustmentDTO adjustmentDTO);
}


