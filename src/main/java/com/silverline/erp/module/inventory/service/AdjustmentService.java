package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.StockAdjustmentDTO;

import java.util.List;

public interface AdjustmentService {

    List<StockAdjustmentDTO> getAllAdjustments(Long branchId, Long productId);

    StockAdjustmentDTO createAdjustment(StockAdjustmentDTO adjustmentDTO);
}


