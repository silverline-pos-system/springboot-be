package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.StockDTO;

import java.util.List;

public interface StockOverviewService {

    List<StockDTO> getStockOverview(Long branchId);

    List<StockDTO> getLowStockProducts(Long branchId, Integer threshold);
}


