package com.nsbm.rocs.modules.inventory.service.impl;

import com.nsbm.rocs.entity.inventory.Batch;
import com.nsbm.rocs.modules.inventory.dto.ExpiryAlertDTO;
import com.nsbm.rocs.modules.inventory.repository.BatchRepository;
import com.nsbm.rocs.modules.inventory.repository.ProductRepository;
import com.nsbm.rocs.modules.inventory.service.ExpiryCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpiryCalendarServiceImpl implements ExpiryCalendarService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;

    @Override
    public List<ExpiryAlertDTO> getExpiryCalendar(LocalDate start, LocalDate end, Long branchId) {
        LocalDate startDate = start != null ? start : LocalDate.now();
        LocalDate endDate = end != null ? end : startDate.plusMonths(3);

        List<Batch> batches = batchRepository.findExpiringSoonBatches(startDate, endDate);

        return batches.stream()
                .filter(batch -> branchId == null || batch.getBranchId().equals(branchId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpiryAlertDTO> getExpiringSoon(Long branchId, Integer daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(daysAhead);

        List<Batch> batches = batchRepository.findExpiringSoonBatches(today, futureDate);

        return batches.stream()
                .filter(batch -> branchId == null || batch.getBranchId().equals(branchId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpiryAlertDTO> getExpired(Long branchId) {
        LocalDate today = LocalDate.now();
        List<Batch> batches = batchRepository.findExpiredBatches(today);

        return batches.stream()
                .filter(batch -> branchId == null || batch.getBranchId().equals(branchId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ExpiryAlertDTO convertToDTO(Batch batch) {
        ExpiryAlertDTO dto = new ExpiryAlertDTO();
        dto.setBatchId(batch.getBatchId());
        dto.setBatchCode(batch.getBatchCode());
        dto.setProductId(batch.getProductId());
        dto.setBranchId(batch.getBranchId());
        dto.setQty(batch.getQty());
        dto.setExpiryDate(batch.getExpiryDate());

        if (batch.getExpiryDate() != null) {
            long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
            dto.setDaysToExpiry(daysToExpiry);

            if (daysToExpiry < 0) {
                dto.setAlertLevel("CRITICAL");
                dto.setMessage("Product has expired");
            } else if (daysToExpiry <= 7) {
                dto.setAlertLevel("CRITICAL");
                dto.setMessage("Expires within a week");
            } else if (daysToExpiry <= 30) {
                dto.setAlertLevel("WARNING");
                dto.setMessage("Expires within a month");
            } else {
                dto.setAlertLevel("INFO");
                dto.setMessage("Expires in " + daysToExpiry + " days");
            }
        }

        productRepository.findById(batch.getProductId()).ifPresent(product -> {
            dto.setProductName(product.getName());
            dto.setProductSku(product.getSku());
        });

        return dto;
    }
}


