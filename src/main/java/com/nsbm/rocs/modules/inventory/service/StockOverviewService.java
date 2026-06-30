package com.nsbm.rocs.modules.inventory.service;

import com.nsbm.rocs.modules.inventory.dto.StockDTO;

import java.util.List;

public interface StockOverviewService {

    List<StockDTO> getStockOverview(Long branchId);

    List<StockDTO> getLowStockProducts(Long branchId, Integer threshold);
}


