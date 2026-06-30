package com.silverline.erp.module.inventory.service.impl;

import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.module.inventory.dto.StockAdjustmentDTO;
import com.silverline.erp.common.exception.InsufficientStockException;
import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.StockRepository;
import com.silverline.erp.module.inventory.service.AdjustmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdjustmentServiceImpl implements AdjustmentService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final List<StockAdjustmentDTO> adjustmentHistory = new ArrayList<>();

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentDTO> getAllAdjustments(Long branchId, Long productId) {
        return adjustmentHistory.stream()
                .filter(adj -> branchId == null || adj.getBranchId().equals(branchId))
                .filter(adj -> productId == null || adj.getProductId().equals(productId))
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
            return newStock;
        });

        BigDecimal adjustmentQty = adjustmentDTO.getQuantity();

        switch (adjustmentDTO.getAdjustmentType()) {
            case "ADD":
            case "RETURN":
            case "CORRECTION":
                if (adjustmentQty.compareTo(BigDecimal.ZERO) > 0) {
                    stock.setQuantity(stock.getQuantity().add(adjustmentQty));
                } else if (adjustmentQty.compareTo(BigDecimal.ZERO) < 0) {
                    if (stock.getQuantity().add(adjustmentQty).compareTo(BigDecimal.ZERO) < 0) {
                        throw new InsufficientStockException("Cannot reduce stock below zero");
                    }
                    stock.setQuantity(stock.getQuantity().add(adjustmentQty));
                }
                break;

            case "REMOVE":
            case "DAMAGE":
            case "LOSS":
                BigDecimal removeQty = adjustmentQty.abs();
                if (stock.getQuantity().compareTo(removeQty) < 0) {
                    throw new InsufficientStockException(
                        "Insufficient stock. Available: " + stock.getQuantity() + ", Required: " + removeQty
                    );
                }
                stock.setQuantity(stock.getQuantity().subtract(removeQty));
                break;

            default:
                throw new IllegalArgumentException("Invalid adjustment type: " + adjustmentDTO.getAdjustmentType());
        }

        stockRepository.save(stock);
        adjustmentHistory.add(adjustmentDTO);

        return adjustmentDTO;
    }
}

