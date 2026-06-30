package com.silverline.erp.module.repair.service;

import com.silverline.erp.domain.service.RepairJob;
import com.silverline.erp.module.repair.dto.RepairJobRequestDTO;

import java.util.List;
import java.util.Map;

public interface RepairJobService {
    RepairJob logRepairJob(RepairJobRequestDTO requestDTO);
    List<RepairJob> getAllRepairs();
    List<RepairJob> getRepairsByBranch(Long branchId);
    RepairJob finalizeRepairCost(Long repairId, java.math.BigDecimal finalCost, Long managerId);
    RepairJob updateRepairStatus(Long repairId, String status, Long technicianId, String notes);
    RepairJob requestFinalizeCost(Long repairId, Long managerId, java.math.BigDecimal estimatedCost, String costNote);
    List<Map<String, Object>> searchRepairs(String query);
    RepairJob markRepairPaid(Long repairId, java.math.BigDecimal amount, String paymentMethod, Long receivedBy);
}


