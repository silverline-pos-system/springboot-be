package com.silverline.erp.module.inventory.service.impl;

import com.silverline.erp.common.exception.InsufficientStockException;
import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.inventory.StockAdjustment;
import com.silverline.erp.module.inventory.dto.StockAdjustmentDTO;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.StockAdjustmentRepository;
import com.silverline.erp.module.inventory.repository.StockRepository;
import com.silverline.erp.module.inventory.service.AdjustmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdjustmentServiceImpl implements AdjustmentService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentDTO> getAllAdjustments(Long branchId, Long productId) {
        return stockAdjustmentRepository.search(branchId, productId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public StockAdjustmentDTO createAdjustment(StockAdjustmentDTO adjustmentDTO) {
        if (!productRepository.existsById(adjustmentDTO.getProductId())) {
            throw new ResourceNotFoundException("Product not found with id: " + adjustmentDTO.getProductId());
        }

        Stock stock = stockRepository.findByBranchIdAndProductId(
                adjustmentDTO.getBranchId(),
                adjustmentDTO.getProductId()
        ).orElseGet(() -> {
            Stock newStock = new Stock();
            newStock.setBranchId(adjustmentDTO.getBranchId());
            newStock.setProductId(adjustmentDTO.getProductId());
            newStock.setQuantity(BigDecimal.ZERO);
            newStock.setReservedQty(BigDecimal.ZERO);
            newStock.setAvailableQty(BigDecimal.ZERO);
            return newStock;
        });

        BigDecimal before = stock.getQuantity() != null ? stock.getQuantity() : BigDecimal.ZERO;
        BigDecimal adjustmentQty = adjustmentDTO.getQuantity();
        BigDecimal after;

        switch (adjustmentDTO.getAdjustmentType()) {
            case "ADD":
            case "RETURN":
            case "CORRECTION":
                after = before.add(adjustmentQty);
                if (after.compareTo(BigDecimal.ZERO) < 0) {
                    throw new InsufficientStockException("Cannot reduce stock below zero");
                }
                break;

            case "REMOVE":
            case "DAMAGE":
            case "LOSS":
                BigDecimal removeQty = adjustmentQty.abs();
                if (before.compareTo(removeQty) < 0) {
                    throw new InsufficientStockException(
                            "Insufficient stock. Available: " + before + ", Required: " + removeQty
                    );
                }
                after = before.subtract(removeQty);
                break;

            default:
                throw new IllegalArgumentException("Invalid adjustment type: " + adjustmentDTO.getAdjustmentType());
        }

        stock.setQuantity(after);
        BigDecimal reserved = stock.getReservedQty() != null ? stock.getReservedQty() : BigDecimal.ZERO;
        stock.setAvailableQty(after.subtract(reserved));
        stockRepository.save(stock);

        // Persist the audit record so adjustments survive restarts and are traceable.
        StockAdjustment record = StockAdjustment.builder()
                .branchId(adjustmentDTO.getBranchId())
                .productId(adjustmentDTO.getProductId())
                .adjustmentType(adjustmentDTO.getAdjustmentType())
                .quantity(adjustmentQty)
                .quantityBefore(before)
                .quantityAfter(after)
                .reason(adjustmentDTO.getReason())
                .createdBy(adjustmentDTO.getAdjustedBy())
                .build();
        stockAdjustmentRepository.save(record);

        return adjustmentDTO;
    }

    private StockAdjustmentDTO toDTO(StockAdjustment a) {
        return StockAdjustmentDTO.builder()
                .branchId(a.getBranchId())
                .productId(a.getProductId())
                .quantity(a.getQuantity())
                .adjustmentType(a.getAdjustmentType())
                .reason(a.getReason())
                .adjustedBy(a.getCreatedBy())
                .build();
    }
}
