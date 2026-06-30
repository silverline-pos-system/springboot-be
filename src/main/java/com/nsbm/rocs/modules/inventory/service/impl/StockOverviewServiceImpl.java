package com.nsbm.rocs.modules.inventory.service.impl;

import com.nsbm.rocs.entity.inventory.Stock;
import com.nsbm.rocs.modules.inventory.dto.StockDTO;
import com.nsbm.rocs.modules.inventory.repository.StockRepository;
import com.nsbm.rocs.modules.inventory.repository.ProductRepository;
import com.nsbm.rocs.modules.inventory.service.StockOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockOverviewServiceImpl implements StockOverviewService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;

    @Override
    public List<StockDTO> getStockOverview(Long branchId) {
        List<Stock> stocks = branchId != null
            ? stockRepository.findByBranchId(branchId)
            : stockRepository.findAll();

        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockDTO> getLowStockProducts(Long branchId, Integer threshold) {
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID is required for low stock query");
        }

        int limit = (threshold != null) ? threshold : 10;
        List<Stock> lowStocks = stockRepository.findLowStockProducts(branchId, limit);
        return lowStocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private StockDTO convertToDTO(Stock stock) {
        StockDTO dto = new StockDTO();
        dto.setStockId(stock.getStockId());
        dto.setBranchId(stock.getBranchId());
        dto.setProductId(stock.getProductId());
        dto.setQuantity(stock.getQuantity());
        dto.setReservedQty(stock.getReservedQty());
        dto.setAvailableQty(stock.getAvailableQty());
        dto.setLastUpdated(stock.getLastUpdated());

        // Fetch product details
        productRepository.findById(stock.getProductId()).ifPresent(product -> {
            dto.setProductName(product.getName());
            dto.setProductSku(product.getSku());
        });

        return dto;
    }
}


