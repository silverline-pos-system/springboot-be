package com.silverline.erp.module.inventory.service.impl;

import com.silverline.erp.common.exception.InsufficientStockException;
import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.dto.BatchDTO;
import com.silverline.erp.module.inventory.dto.ExpiryAlertDTO;
import com.silverline.erp.module.inventory.repository.BatchRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;

    private Pageable capPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int cappedSize = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), cappedSize, pageable.getSort());
    }

    @Override
    public Page<BatchDTO> getAllBatches(Pageable pageable) {
        Pageable capped = capPageable(pageable);
        return batchRepository.findAll(capped).map(this::convertToDTO);
    }

    @Override
    public List<BatchDTO> getBatchesByProduct(Long productId) {
        return batchRepository.findByProductId(productId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BatchDTO> getBatchesByBranch(Long branchId) {
        return batchRepository.findByBranchId(branchId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BatchDTO getBatchById(Long id) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));
        return convertToDTO(batch);
    }

    @Override
    public BatchDTO getBatchByCode(Long branchId, Long productId, String batchCode) {
        List<Batch> candidates = batchRepository.findAllByBranchIdAndProductIdAndBatchCodeOrdered(branchId, productId, batchCode);
        Batch batch = candidates.stream()
                .filter(b -> b.getQty() != null && b.getQty().compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.comparing((Batch b) -> b.getExpiryDate() == null)
                        .thenComparing(Batch::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Batch::getManufacturingDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Batch::getBatchId))
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with code: " + batchCode));
        return convertToDTO(batch);
    }

    @Override
    public List<BatchDTO> getExpiredBatches() {
        return batchRepository.findExpiredBatches(LocalDate.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BatchDTO> getExpiringSoonBatches(int days) {
        LocalDate futureDate = LocalDate.now().plusDays(days);
        return batchRepository.findExpiringSoonBatches(LocalDate.now(), futureDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BatchDTO createBatch(BatchDTO batchDTO) {
        productRepository.findById(batchDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + batchDTO.getProductId()));

        Batch batch = convertToEntity(batchDTO);
        Batch savedBatch = batchRepository.save(batch);
        return convertToDTO(savedBatch);
    }

    @Override
    public BatchDTO updateBatch(Long id, BatchDTO batchDTO) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));

        batch.setProductId(batchDTO.getProductId());
        batch.setBranchId(batchDTO.getBranchId());
        batch.setBatchCode(batchDTO.getBatchCode());
        batch.setManufacturingDate(batchDTO.getManufacturingDate());
        batch.setExpiryDate(batchDTO.getExpiryDate());
        batch.setQty(batchDTO.getQty());
        batch.setCostPrice(batchDTO.getCostPrice());
        batch.setSellingPrice(batchDTO.getSellingPrice());
        batch.setMrp(batchDTO.getMrp());

        Batch updatedBatch = batchRepository.save(batch);
        return convertToDTO(updatedBatch);
    }

    @Override
    public void deleteBatch(Long id) {
        if (!batchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Batch not found with id: " + id);
        }
        batchRepository.deleteById(id);
    }

    @Override
    public List<ExpiryAlertDTO> getExpiryAlerts(int warningDays, int criticalDays) {
        LocalDate now = LocalDate.now();
        LocalDate warningDate = now.plusDays(warningDays);

        List<Batch> batches = batchRepository.findExpiringSoonBatches(now, warningDate);
        List<ExpiryAlertDTO> alerts = new ArrayList<>();

        for (Batch batch : batches) {
            if (batch.getExpiryDate() == null || batch.getQty() == null || batch.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Product product = productRepository.findById(batch.getProductId()).orElse(null);
            if (product == null) continue;

            ExpiryAlertDTO alert = new ExpiryAlertDTO();
            alert.setBatchId(batch.getBatchId());
            alert.setBatchCode(batch.getBatchCode());
            alert.setProductId(product.getProductId());
            alert.setProductName(product.getName());
            alert.setProductSku(product.getSku());
            alert.setBranchId(batch.getBranchId());
            alert.setQty(batch.getQty());
            alert.setExpiryDate(batch.getExpiryDate());

            long daysToExpiry = ChronoUnit.DAYS.between(now, batch.getExpiryDate());
            alert.setDaysToExpiry(daysToExpiry);

            if (daysToExpiry < 0) {
                alert.setAlertLevel("EXPIRED");
                alert.setMessage("Batch expired");
            } else if (daysToExpiry <= criticalDays) {
                alert.setAlertLevel("CRITICAL");
                alert.setMessage("Batch expiring within " + daysToExpiry + " days");
            } else if (daysToExpiry <= warningDays) {
                alert.setAlertLevel("WARNING");
                alert.setMessage("Batch expiring soon in " + daysToExpiry + " days");
            } else {
                alert.setAlertLevel("INFO");
                alert.setMessage("Batch healthy");
            }

            alerts.add(alert);
        }

        return alerts;
    }

    @Override
    @Transactional
    public void deductByFEFO(Long branchId, Long productId, int qtyToDeduct) {
        if (branchId == null || productId == null || qtyToDeduct <= 0) {
            throw new IllegalArgumentException("branchId, productId and qtyToDeduct must be provided and qtyToDeduct > 0");
        }

        List<Batch> batches = batchRepository.findAvailableByBranchAndProductFefo(branchId, productId, LocalDate.now());
        if (batches.isEmpty()) {
            throw new InsufficientStockException("Not enough batch quantity to deduct " + qtyToDeduct);
        }

        BigDecimal remaining = BigDecimal.valueOf(qtyToDeduct);
        List<Batch> toUpdate = new ArrayList<>();

        for (Batch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal take = batch.getQty().min(remaining);
            if (take.compareTo(BigDecimal.ZERO) > 0) {
                batch.setQty(batch.getQty().subtract(take));
                remaining = remaining.subtract(take);
                toUpdate.add(batch);
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new InsufficientStockException("Not enough batch quantity to deduct " + qtyToDeduct);
        }

        batchRepository.saveAll(toUpdate);
    }

    @Override
    public List<BatchDTO> getExpiredBatchesByBranchAndProduct(Long branchId, Long productId) {
        return batchRepository.findExpiredByBranchAndProduct(branchId, productId, LocalDate.now())
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<BatchDTO> getExpiringSoonByBranchAndProduct(Long branchId, Long productId, int days) {
        LocalDate now = LocalDate.now();
        LocalDate end = now.plusDays(days);
        return batchRepository.findExpiringSoonByBranchAndProduct(branchId, productId, now, end)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<BatchDTO> getFEFOBatches(Long productId, Long branchId) {
        List<Batch> batches = batchRepository.findAvailableByBranchAndProductFefo(
                branchId, productId, LocalDate.now());
        return batches.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private BatchDTO convertToDTO(Batch batch) {
        BatchDTO dto = new BatchDTO();
        dto.setBatchId(batch.getBatchId());
        dto.setProductId(batch.getProductId());
        dto.setBranchId(batch.getBranchId());
        dto.setBatchCode(batch.getBatchCode());
        dto.setManufacturingDate(batch.getManufacturingDate());
        dto.setExpiryDate(batch.getExpiryDate());
        dto.setQty(batch.getQty());
        dto.setCostPrice(batch.getCostPrice());
        dto.setSellingPrice(batch.getSellingPrice());
        dto.setMrp(batch.getMrp());

        productRepository.findById(batch.getProductId()).ifPresent(product -> {
            dto.setProductName(product.getName());
            dto.setProductSku(product.getSku());
        });

        if (batch.getExpiryDate() != null) {
            long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
            dto.setDaysToExpiry(daysToExpiry);

            if (daysToExpiry < 0) {
                dto.setExpiryStatus("EXPIRED");
            } else if (daysToExpiry <= 30) {
                dto.setExpiryStatus("EXPIRING_SOON");
            } else {
                dto.setExpiryStatus("FRESH");
            }
        }

        return dto;
    }

    private Batch convertToEntity(BatchDTO dto) {
        Batch batch = new Batch();
        batch.setBatchId(dto.getBatchId());
        batch.setProductId(dto.getProductId());
        batch.setBranchId(dto.getBranchId());
        batch.setBatchCode(dto.getBatchCode());
        batch.setManufacturingDate(dto.getManufacturingDate());
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setQty(dto.getQty());
        batch.setCostPrice(dto.getCostPrice());
        batch.setSellingPrice(dto.getSellingPrice());
        batch.setMrp(dto.getMrp());
        return batch;
    }

    @Override
    public void deductBatchStock(Long batchId, BigDecimal qty) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));
        batch.setQty(batch.getQty().subtract(qty));
        batchRepository.save(batch);
    }
}
