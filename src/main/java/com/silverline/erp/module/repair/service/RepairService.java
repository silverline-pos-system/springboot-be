package com.silverline.erp.module.repair.service;

import com.silverline.erp.domain.repair.RepairJob;
import com.silverline.erp.module.repair.dto.RepairJobRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface RepairService {
    RepairJob logRepairJob(RepairJobRequestDTO requestDTO);

    Page<RepairJob> getAllRepairs(Pageable pageable);

    List<RepairJob> getRepairsByBranch(Long branchId);

    RepairJob finalizeRepairCost(Long repairId, java.math.BigDecimal finalCost, Long managerId);

    RepairJob updateRepairStatus(Long repairId, String status, Long technicianId, String notes);

    RepairJob requestFinalizeCost(Long repairId, Long managerId, java.math.BigDecimal estimatedCost, String costNote);

    List<Map<String, Object>> searchRepairs(String query);

    RepairJob markRepairPaid(Long repairId, java.math.BigDecimal amount, String paymentMethod, Long receivedBy);
}
