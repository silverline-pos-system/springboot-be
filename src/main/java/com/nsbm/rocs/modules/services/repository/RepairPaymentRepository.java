package com.nsbm.rocs.modules.services.repository;

import com.nsbm.rocs.entity.services.RepairPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairPaymentRepository extends JpaRepository<RepairPayment, Long> {
    List<RepairPayment> findByRepairId(Long repairId);
}

