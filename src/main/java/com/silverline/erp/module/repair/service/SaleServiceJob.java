package com.silverline.erp.module.repair.service;

import com.silverline.erp.domain.service.SaleService;
import com.silverline.erp.module.repair.dto.SaleServiceRequestDTO;

import java.util.List;

import java.math.BigDecimal;

public interface SaleServiceJob {
    SaleService requestDtvService(SaleServiceRequestDTO requestDTO);
    List<SaleService> getAllDtvServices();
    List<SaleService> getDtvServicesByTechnician(Long technicianId);
    SaleService updateDtvStatus(Long serviceId, String newStatus, Long technicianId, BigDecimal balanceCollected, String additionalItems);
}

