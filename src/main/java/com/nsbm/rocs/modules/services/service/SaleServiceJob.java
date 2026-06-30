package com.nsbm.rocs.modules.services.service;

import com.nsbm.rocs.entity.services.SaleService;
import com.nsbm.rocs.modules.services.dto.SaleServiceRequestDTO;

import java.util.List;

import java.math.BigDecimal;

public interface SaleServiceJob {
    SaleService requestDtvService(SaleServiceRequestDTO requestDTO);
    List<SaleService> getAllDtvServices();
    List<SaleService> getDtvServicesByTechnician(Long technicianId);
    SaleService updateDtvStatus(Long serviceId, String newStatus, Long technicianId, BigDecimal balanceCollected, String additionalItems);
}

