package com.nsbm.rocs.modules.inventory.service;

import com.nsbm.rocs.entity.inventory.Stock;
import com.nsbm.rocs.modules.inventory.dto.StockDTO;
import com.nsbm.rocs.modules.inventory.dto.StockAdjustmentDTO;
import com.nsbm.rocs.modules.inventory.dto.StockReportDTO;
import com.nsbm.rocs.modules.inventory.dto.LowStockAlertDTO;
import com.nsbm.rocs.common.exception.ResourceNotFoundException;
import com.nsbm.rocs.modules.inventory.repository.StockRepository;
import com.nsbm.rocs.modules.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;

    public List<StockDTO> getAllStock() {
        return stockRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<StockDTO> getStockByBranch(Long branchId) {
        return stockRepository.findByBranchId(branchId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<StockDTO> getStockByProduct(Long productId) {
        return stockRepository.findByProductId(productId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public StockDTO getStockByBranchAndProduct(Long branchId, Long productId) {
        return stockRepository.findByBranchIdAndProductId(branchId, productId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));
    }

    public StockDTO adjustStock(StockAdjustmentDTO adjustmentDTO) {
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

        BigDecimal newQty;
        switch (adjustmentDTO.getAdjustmentType().toUpperCase()) {
            case "ADD":
                newQty = stock.getQuantity().add(adjustmentDTO.getQuantity());
                break;
            case "SUBTRACT":
                newQty = stock.getQuantity().subtract(adjustmentDTO.getQuantity());
                if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("Insufficient stock");
                }
                break;
            case "SET":
                newQty = adjustmentDTO.getQuantity();
                break;
            default:
                throw new RuntimeException("Invalid adjustment type: " + adjustmentDTO.getAdjustmentType());
        }

        stock.setQuantity(newQty);
        stock.setAvailableQty(newQty.subtract(stock.getReservedQty()));

        Stock saved = stockRepository.save(stock);
        return mapToDTO(saved);
    }

    public List<StockDTO> getLowStock(Long branchId) {
        return stockRepository.findLowStockByBranch(branchId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<StockDTO> getOutOfStock(Long branchId) {
        return stockRepository.findOutOfStockByBranch(branchId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public StockDTO addStock(Long branchId, Long productId, Integer quantity) {
        StockAdjustmentDTO adjustment = new StockAdjustmentDTO();
        adjustment.setBranchId(branchId);
        adjustment.setProductId(productId);
        adjustment.setQuantity(BigDecimal.valueOf(quantity));
        adjustment.setAdjustmentType("ADD");
        return adjustStock(adjustment);
    }

    public StockDTO removeStock(Long branchId, Long productId, Integer quantity) {
        StockAdjustmentDTO adjustment = new StockAdjustmentDTO();
        adjustment.setBranchId(branchId);
        adjustment.setProductId(productId);
        adjustment.setQuantity(BigDecimal.valueOf(quantity));
        adjustment.setAdjustmentType("SUBTRACT");
        return adjustStock(adjustment);
    }

    public StockDTO reserveStock(Long branchId, Long productId, Integer quantity) {
        Stock stock = stockRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

        BigDecimal newReserved = stock.getReservedQty().add(BigDecimal.valueOf(quantity));
        if (newReserved.compareTo(stock.getQuantity()) > 0) {
            throw new RuntimeException("Cannot reserve more than available quantity");
        }
        stock.setReservedQty(newReserved);
        stock.setAvailableQty(stock.getQuantity().subtract(newReserved));
        return mapToDTO(stockRepository.save(stock));
    }

    public StockDTO releaseReservedStock(Long branchId, Long productId, Integer quantity) {
        Stock stock = stockRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

        BigDecimal newReserved = stock.getReservedQty().subtract(BigDecimal.valueOf(quantity));
        if (newReserved.compareTo(BigDecimal.ZERO) < 0) {
            newReserved = BigDecimal.ZERO;
        }
        stock.setReservedQty(newReserved);
        stock.setAvailableQty(stock.getQuantity().subtract(newReserved));
        return mapToDTO(stockRepository.save(stock));
    }

    public List<StockReportDTO> getStockReport(Long branchId) {
        return stockRepository.findByBranchId(branchId).stream()
                .map(stock -> {
                    StockReportDTO report = StockReportDTO.builder()
                            .productId(stock.getProductId())
                            .branchId(stock.getBranchId())
                            .quantity(stock.getQuantity())
                            .reservedQty(stock.getReservedQty())
                            .availableQty(stock.getAvailableQty())
                            .build();

                    productRepository.findById(stock.getProductId()).ifPresent(product -> {
                        report.setProductName(product.getName());
                        report.setProductSku(product.getSku());
                        report.setReorderLevel(product.getReorderLevel());
                        report.setIsLowStock(stock.getAvailableQty().compareTo(product.getReorderLevel()) <= 0);
                        report.setIsOutOfStock(stock.getAvailableQty().compareTo(BigDecimal.ZERO) <= 0);
                        report.setStockValue(stock.getQuantity().multiply(product.getCostPrice()));
                    });

                    return report;
                })
                .collect(Collectors.toList());
    }

    public List<LowStockAlertDTO> getLowStockAlerts(Long branchId) {
        return stockRepository.findLowStockByBranch(branchId).stream()
                .map(stock -> {
                    LowStockAlertDTO alert = LowStockAlertDTO.builder()
                            .productId(stock.getProductId())
                            .branchId(stock.getBranchId())
                            .currentQuantity(stock.getAvailableQty())
                            .build();

                    productRepository.findById(stock.getProductId()).ifPresent(product -> {
                        alert.setProductName(product.getName());
                        alert.setProductSku(product.getSku());
                        alert.setReorderLevel(product.getReorderLevel());
                        alert.setShortage(product.getReorderLevel().subtract(stock.getAvailableQty()));

                        if (stock.getAvailableQty().compareTo(BigDecimal.ZERO) <= 0) {
                            alert.setAlertLevel("OUT_OF_STOCK");
                        } else if (stock.getAvailableQty().compareTo(product.getReorderLevel().multiply(BigDecimal.valueOf(0.5))) <= 0) {
                            alert.setAlertLevel("CRITICAL");
                        } else {
                            alert.setAlertLevel("LOW");
                        }
                    });

                    return alert;
                })
                .collect(Collectors.toList());
    }

    private StockDTO mapToDTO(Stock stock) {
        StockDTO dto = StockDTO.builder()
                .stockId(stock.getStockId())
                .branchId(stock.getBranchId())
                .productId(stock.getProductId())
                .quantity(stock.getQuantity())
                .reservedQty(stock.getReservedQty())
                .availableQty(stock.getAvailableQty())
                .lastUpdated(stock.getLastUpdated())
                .build();

        // Fetch product details
        productRepository.findById(stock.getProductId()).ifPresent(product -> {
            dto.setProductName(product.getName());
            dto.setProductSku(product.getSku());
            dto.setReorderLevel(product.getReorderLevel());
            dto.setIsLowStock(stock.getAvailableQty().compareTo(product.getReorderLevel()) <= 0);
        });

        return dto;
    }
}


