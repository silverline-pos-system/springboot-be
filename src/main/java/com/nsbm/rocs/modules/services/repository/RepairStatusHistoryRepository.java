package com.nsbm.rocs.modules.services.repository;

import com.nsbm.rocs.entity.services.RepairStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairStatusHistoryRepository extends JpaRepository<RepairStatusHistory, Long> {
    List<RepairStatusHistory> findByRepairIdOrderByChangedAtDesc(Long repairId);
}

