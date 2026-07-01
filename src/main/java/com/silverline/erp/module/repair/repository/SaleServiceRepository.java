package com.silverline.erp.module.repair.repository;

import com.silverline.erp.domain.enums.ServiceStatus;
import com.silverline.erp.domain.service.SaleService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleServiceRepository extends JpaRepository<SaleService, Long> {
    List<SaleService> findByTechnicianId(Long technicianId);
    List<SaleService> findByServiceStatus(ServiceStatus status);
}

