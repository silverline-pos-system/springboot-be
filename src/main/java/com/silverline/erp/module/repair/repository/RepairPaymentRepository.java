package com.silverline.erp.module.repair.repository;

import com.silverline.erp.domain.service.RepairPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairPaymentRepository extends JpaRepository<RepairPayment, Long> {
    List<RepairPayment> findByRepairId(Long repairId);
}

