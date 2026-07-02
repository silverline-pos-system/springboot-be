package com.silverline.erp.module.inventory.service.impl;

import com.silverline.erp.common.event.StockAdjustedEvent;
import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.dto.LowStockAlertDTO;
import com.silverline.erp.module.inventory.dto.StockAdjustmentDTO;
import com.silverline.erp.module.inventory.dto.StockDTO;
import com.silverline.erp.module.inventory.dto.StockReportDTO;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StockServiceImpl stockService;

    private Stock stock;
    private Product product;

    @BeforeEach
    void setUp() {
        stock = new Stock();
        stock.setStockId(1L);
        stock.setBranchId(10L);
        stock.setProductId(20L);
        stock.setQuantity(BigDecimal.valueOf(50));
        stock.setReservedQty(BigDecimal.valueOf(10));
        stock.setAvailableQty(BigDecimal.valueOf(40));

        product = new Product();
        product.setProductId(20L);
        product.setName("Test SKU");
        product.setSku("SKU-1234");
        product.setReorderLevel(BigDecimal.valueOf(15));
        product.setCostPrice(BigDecimal.valueOf(5));
    }

    @Test
    void adjustStock_Add_Success() {
        // Arrange
        StockAdjustmentDTO adjustment = new StockAdjustmentDTO();
        adjustment.setBranchId(10L);
        adjustment.setProductId(20L);
        adjustment.setQuantity(BigDecimal.valueOf(10));
        adjustment.setAdjustmentType("ADD");

        when(stockRepository.findByBranchIdAndProductId(10L, 20L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        StockDTO result = stockService.adjustStock(adjustment);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(60), result.getQuantity());
        assertEquals(BigDecimal.valueOf(50), result.getAvailableQty());
        verify(eventPublisher).publishEvent(any(StockAdjustedEvent.class));
    }

    @Test
    void adjustStock_Subtract_Success() {
        // Arrange
        StockAdjustmentDTO adjustment = new StockAdjustmentDTO();
        adjustment.setBranchId(10L);
        adjustment.setProductId(20L);
        adjustment.setQuantity(BigDecimal.valueOf(10));
        adjustment.setAdjustmentType("SUBTRACT");

        when(stockRepository.findByBranchIdAndProductId(10L, 20L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        StockDTO result = stockService.adjustStock(adjustment);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(40), result.getQuantity());
        assertEquals(BigDecimal.valueOf(30), result.getAvailableQty());
    }

    @Test
    void adjustStock_Subtract_Insufficient_ThrowsException() {
        // Arrange
        StockAdjustmentDTO adjustment = new StockAdjustmentDTO();
        adjustment.setBranchId(10L);
        adjustment.setProductId(20L);
        adjustment.setQuantity(BigDecimal.valueOf(60));
        adjustment.setAdjustmentType("SUBTRACT");

        when(stockRepository.findByBranchIdAndProductId(10L, 20L)).thenReturn(Optional.of(stock));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                stockService.adjustStock(adjustment)
        );
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    void reserveStock_Success() {
        // Arrange
        when(stockRepository.findByBranchIdAndProductId(10L, 20L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        StockDTO result = stockService.reserveStock(10L, 20L, 5);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(15), result.getReservedQty());
        assertEquals(BigDecimal.valueOf(35), result.getAvailableQty());
    }

    @Test
    void releaseReservedStock_Success() {
        // Arrange
        when(stockRepository.findByBranchIdAndProductId(10L, 20L)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        StockDTO result = stockService.releaseReservedStock(10L, 20L, 5);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(5), result.getReservedQty());
        assertEquals(BigDecimal.valueOf(45), result.getAvailableQty());
    }

    @Test
    void getStockReport_Success() {
        // Arrange
        when(stockRepository.findByBranchId(10L)).thenReturn(Collections.singletonList(stock));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        // Act
        List<StockReportDTO> report = stockService.getStockReport(10L);

        // Assert
        assertNotNull(report);
        assertEquals(1, report.size());
        StockReportDTO r = report.getFirst();
        assertEquals("Test SKU", r.getProductName());
        assertEquals(BigDecimal.valueOf(250), r.getStockValue()); // 50 qty * 5 costPrice = 250
    }

    @Test
    void isStockAvailable_True() {
        // Arrange
        when(stockRepository.findByBranchIdAndProductId(10L, 20L)).thenReturn(Optional.of(stock));

        // Act
        boolean available = stockService.isStockAvailable(10L, 20L, 30);

        // Assert
        assertTrue(available);
    }

    @Test
    void isStockAvailable_False() {
        // Arrange
        when(stockRepository.findByBranchIdAndProductId(10L, 20L)).thenReturn(Optional.of(stock));

        // Act
        boolean available = stockService.isStockAvailable(10L, 20L, 45); // available is 40

        // Assert
        assertFalse(available);
    }
}
