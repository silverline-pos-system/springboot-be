package com.silverline.erp.module.repair.repository;

import com.silverline.erp.domain.repair.RepairStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairStatusHistoryRepository extends JpaRepository<RepairStatusHistory, Long> {
    List<RepairStatusHistory> findByRepairIdOrderByChangedAtDesc(Long repairId);
}

