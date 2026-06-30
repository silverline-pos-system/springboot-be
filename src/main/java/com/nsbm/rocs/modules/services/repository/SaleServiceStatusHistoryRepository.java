package com.nsbm.rocs.modules.services.repository;

import com.nsbm.rocs.entity.services.SaleServiceStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleServiceStatusHistoryRepository extends JpaRepository<SaleServiceStatusHistory, Long> {
    List<SaleServiceStatusHistory> findByServiceIdOrderByChangedAtDesc(Long serviceId);
}

