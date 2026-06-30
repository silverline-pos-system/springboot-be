package com.silverline.erp.module.repair.repository;

import com.silverline.erp.domain.service.SaleServiceStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleServiceStatusHistoryRepository extends JpaRepository<SaleServiceStatusHistory, Long> {
    List<SaleServiceStatusHistory> findByServiceIdOrderByChangedAtDesc(Long serviceId);
}

