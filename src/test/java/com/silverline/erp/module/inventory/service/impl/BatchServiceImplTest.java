package com.silverline.erp.module.inventory.service.impl;

import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.dto.BatchDTO;
import com.silverline.erp.module.inventory.dto.ExpiryAlertDTO;
import com.silverline.erp.module.inventory.repository.BatchRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceImplTest {

    @Mock
    private BatchRepository batchRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BatchServiceImpl batchService;

    private Batch batch;
    private BatchDTO batchDTO;
    private Product product;

    @BeforeEach
    void setUp() {
        batch = new Batch();
        batch.setBatchId(1L);
        batch.setProductId(10L);
        batch.setBranchId(100L);
        batch.setBatchCode("B-111");
        batch.setQty(BigDecimal.valueOf(100));
        batch.setExpiryDate(LocalDate.now().plusDays(15));
        batch.setCostPrice(BigDecimal.valueOf(10));
        batch.setSellingPrice(BigDecimal.valueOf(15));

        batchDTO = new BatchDTO();
        batchDTO.setBatchId(1L);
        batchDTO.setProductId(10L);
        batchDTO.setBranchId(100L);
        batchDTO.setBatchCode("B-111");
        batchDTO.setQty(BigDecimal.valueOf(100));
        batchDTO.setExpiryDate(LocalDate.now().plusDays(15));
        batchDTO.setCostPrice(BigDecimal.valueOf(10));
        batchDTO.setSellingPrice(BigDecimal.valueOf(15));

        product = new Product();
        product.setProductId(10L);
        product.setName("Product Name");
        product.setSku("SKU-999");
    }

    @Test
    void getBatchById_Success() {
        // Arrange
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // Act
        BatchDTO result = batchService.getBatchById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("B-111", result.getBatchCode());
    }

    @Test
    void getBatchById_NotFound_ThrowsException() {
        // Arrange
        when(batchRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                batchService.getBatchById(1L)
        );
    }

    @Test
    void createBatch_Success() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);

        // Act
        BatchDTO result = batchService.createBatch(batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals("B-111", result.getBatchCode());
        verify(batchRepository).save(any(Batch.class));
    }

    @Test
    void createBatch_ProductNotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                batchService.createBatch(batchDTO)
        );
        verify(batchRepository, never()).save(any(Batch.class));
    }

    @Test
    void updateBatch_Success() {
        // Arrange
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);

        // Act
        BatchDTO result = batchService.updateBatch(1L, batchDTO);

        // Assert
        assertNotNull(result);
        verify(batchRepository).save(any(Batch.class));
    }

    @Test
    void deleteBatch_Success() {
        // Arrange
        when(batchRepository.existsById(1L)).thenReturn(true);

        // Act
        batchService.deleteBatch(1L);

        // Assert
        verify(batchRepository).deleteById(1L);
    }

    @Test
    void deductBatchStock_Success() {
        // Arrange
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));

        // Act
        batchService.deductBatchStock(1L, BigDecimal.valueOf(10));

        // Assert
        assertEquals(BigDecimal.valueOf(90), batch.getQty());
        verify(batchRepository).save(batch);
    }

    @Test
    void getExpiryAlerts_Success() {
        // Arrange
        when(batchRepository.findExpiringSoonBatches(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(batch));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        // Act
        List<ExpiryAlertDTO> alerts = batchService.getExpiryAlerts(30, 10);

        // Assert
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals("Product Name", alerts.getFirst().getProductName());
        assertEquals("EXPIRING_SOON", alerts.getFirst().getAlertLevel());
    }
}
