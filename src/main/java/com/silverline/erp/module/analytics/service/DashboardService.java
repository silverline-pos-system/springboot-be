package com.silverline.erp.module.analytics.service;

import com.silverline.erp.module.analytics.dto.DashboardStatsDTO;
import com.silverline.erp.module.manager.dto.PendingDispatchDTO;

import java.util.List;

public interface DashboardService {
    List<DashboardStatsDTO> getDashboardStats(Long branchId);

    List<PendingDispatchDTO> getPendingDispatches(Long branchId);
}
