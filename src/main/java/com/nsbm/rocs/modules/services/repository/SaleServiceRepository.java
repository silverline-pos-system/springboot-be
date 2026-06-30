package com.nsbm.rocs.modules.services.repository;

import com.nsbm.rocs.entity.services.SaleService;
import com.nsbm.rocs.entity.enums.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleServiceRepository extends JpaRepository<SaleService, Long> {
    List<SaleService> findByTechnicianId(Long technicianId);
    List<SaleService> findByServiceStatus(ServiceStatus status);
}

