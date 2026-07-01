package com.silverline.erp.module.repair.service;

import com.silverline.erp.domain.service.SaleService;
import com.silverline.erp.module.repair.dto.SaleServiceRequestDTO;

import java.math.BigDecimal;
import java.util.List;

public interface DtvService {
    SaleService requestDtvService(SaleServiceRequestDTO requestDTO);
    List<SaleService> getAllDtvServices();
    List<SaleService> getDtvServicesByTechnician(Long technicianId);
    SaleService updateDtvStatus(Long serviceId, String newStatus, Long technicianId, BigDecimal balanceCollected, String additionalItems);
}
