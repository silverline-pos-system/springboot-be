package com.silverline.erp.module.repair.service.impl;

import com.silverline.erp.domain.enums.ServiceStatus;
import com.silverline.erp.domain.pos.SaleService;
import com.silverline.erp.domain.pos.SaleServiceStatusHistory;
import com.silverline.erp.module.repair.dto.SaleServiceRequestDTO;
import com.silverline.erp.module.repair.repository.SaleServiceRepository;
import com.silverline.erp.module.repair.repository.SaleServiceStatusHistoryRepository;
import com.silverline.erp.module.repair.service.DtvService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DtvServiceImpl implements DtvService {

    private final SaleServiceRepository saleServiceRepository;
    private final SaleServiceStatusHistoryRepository historyRepository;

    @Override
    @Transactional
    public SaleService requestDtvService(SaleServiceRequestDTO requestDTO) {
        SaleService service = new SaleService();
        service.setSaleId(requestDTO.getSaleId());
        service.setServiceType(requestDTO.getServiceType());
        service.setInstallationRequired(requestDTO.getInstallationRequired() != null ? requestDTO.getInstallationRequired() : false);
        service.setTechnicianId(requestDTO.getTechnicianId());
        service.setServiceCharge(requestDTO.getServiceCharge());
        service.setCustomerName(requestDTO.getCustomerName());
        service.setContactNo(requestDTO.getContactNo());
        service.setAddress(requestDTO.getAddress());

        StringBuilder combinedNotes = new StringBuilder();
        if (requestDTO.getNotes() != null) combinedNotes.append(requestDTO.getNotes());
        if (requestDTO.getAltContactNo() != null && !requestDTO.getAltContactNo().isEmpty()) {
            if (!combinedNotes.isEmpty()) combinedNotes.append(" | ");
            combinedNotes.append("Alt Contact: ").append(requestDTO.getAltContactNo());
        }
        if (requestDTO.getPaymentOk() != null && requestDTO.getPaymentOk()) {
            if (!combinedNotes.isEmpty()) combinedNotes.append(" | ");
            combinedNotes.append("Customer Paid Upfront.");
        } else if (requestDTO.getAdvancePayment() != null) {
            if (!combinedNotes.isEmpty()) combinedNotes.append(" | ");
            combinedNotes.append("Customer Paid Advance: LKR ").append(requestDTO.getAdvancePayment());
        }
        service.setNotes(combinedNotes.toString());

        service.setServiceStatus(ServiceStatus.PENDING);

        SaleService savedService = saleServiceRepository.save(service);

        SaleServiceStatusHistory history = new SaleServiceStatusHistory();
        history.setServiceId(savedService.getServiceId());
        history.setOldStatus(null);
        history.setNewStatus(ServiceStatus.PENDING);
        history.setChangedBy(requestDTO.getCreatedBy());
        history.setNotes("DTV Service Request originated from POS.");
        historyRepository.save(history);

        return savedService;
    }

    @Override
    public List<SaleService> getAllDtvServices() {
        return saleServiceRepository.findAll();
    }

    @Override
    public List<SaleService> getDtvServicesByTechnician(Long technicianId) {
        return saleServiceRepository.findByTechnicianId(technicianId);
    }

    @Override
    @Transactional
    public SaleService updateDtvStatus(Long serviceId, String newStatus, Long technicianId, BigDecimal balanceCollected, String additionalItems) {
        SaleService service = saleServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        ServiceStatus oldStatus = service.getServiceStatus();
        ServiceStatus nextStatus = ServiceStatus.valueOf(newStatus.toUpperCase());

        service.setServiceStatus(nextStatus);
        if (technicianId != null) {
            service.setTechnicianId(technicianId);
        }

        if (balanceCollected != null) {
            service.setBalanceCollected(balanceCollected);
        }
        if (additionalItems != null) {
            service.setAdditionalItems(additionalItems);
        }

        SaleService savedService = saleServiceRepository.save(service);

        SaleServiceStatusHistory history = new SaleServiceStatusHistory();
        history.setServiceId(serviceId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(nextStatus);
        history.setChangedBy(technicianId != null ? technicianId : 1L);
        history.setNotes("Status updated to " + nextStatus);
        historyRepository.save(history);

        return savedService;
    }
}
