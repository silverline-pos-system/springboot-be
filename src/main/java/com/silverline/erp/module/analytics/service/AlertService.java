package com.silverline.erp.module.analytics.service;

import com.silverline.erp.module.analytics.dto.BranchAlertDTO;
import com.silverline.erp.module.analytics.dto.ExpiryAlertDTO;
import com.silverline.erp.module.analytics.dto.StockAlertDTO;

import java.util.List;

public interface AlertService {
    List<StockAlertDTO> getStockAlerts(Long branchId);
    List<ExpiryAlertDTO> getExpiryAlerts(Long branchId);
    List<BranchAlertDTO> getBranchAlerts(Long branchId);
}
