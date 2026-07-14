package com.silverline.erp.module.inventory.service.impl;

import com.silverline.erp.common.exception.DuplicateResourceException;
import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.dto.ProductDTO;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProductId(10L);
        product.setSku("SKU001");
        product.setBarcode("1234567890123");
        product.setName("Test Product");
        product.setCostPrice(BigDecimal.valueOf(10));
        product.setSellingPrice(BigDecimal.valueOf(20));
        product.setIsActive(true);

        productDTO = new ProductDTO();
        productDTO.setProductId(10L);
        productDTO.setSku("SKU001");
        productDTO.setBarcode("1234567890123");
        productDTO.setName("Test Product");
        productDTO.setCostPrice(BigDecimal.valueOf(10));
        productDTO.setSellingPrice(BigDecimal.valueOf(20));
        productDTO.setIsActive(true);
        productDTO.setTrackingType("NORMAL");
    }

    @Test
    void getProductById_Success() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(stockRepository.getTotalStockByProduct(10L)).thenReturn(BigDecimal.TEN);

        // Act
        ProductDTO result = productService.getProductById(10L);

        // Assert
        assertNotNull(result);
        assertEquals("SKU001", result.getSku());
    }

    @Test
    void getProductById_NotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                productService.getProductById(10L)
        );
    }

    @Test
    void createProduct_Success() {
        // Arrange
        when(productRepository.findBySku("SKU001")).thenReturn(Optional.empty());
        when(productRepository.findByBarcode("1234567890123")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(stockRepository.getTotalStockByProduct(10L)).thenReturn(BigDecimal.TEN);

        // Act
        ProductDTO result = productService.createProduct(productDTO);

        // Assert
        assertNotNull(result);
        assertEquals("SKU001", result.getSku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_DuplicateSku_ThrowsException() {
        // Arrange
        when(productRepository.findBySku("SKU001")).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () ->
                productService.createProduct(productDTO)
        );
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_DuplicateBarcode_ThrowsException() {
        // Arrange
        when(productRepository.findBySku("SKU001")).thenReturn(Optional.empty());
        when(productRepository.findByBarcode("1234567890123")).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () ->
                productService.createProduct(productDTO)
        );
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_Success() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(stockRepository.getTotalStockByProduct(10L)).thenReturn(BigDecimal.TEN);

        // Act
        ProductDTO result = productService.updateProduct(10L, productDTO);

        // Assert
        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deleteProduct_Success() {
        // Arrange
        when(productRepository.existsById(10L)).thenReturn(true);

        // Act
        productService.deleteProduct(10L);

        // Assert
        verify(productRepository).deleteById(10L);
    }

    @Test
    void deleteProduct_NotFound_ThrowsException() {
        // Arrange
        when(productRepository.existsById(10L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                productService.deleteProduct(10L)
        );
        verify(productRepository, never()).deleteById(10L);
    }

    @Test
    void getNextSku_Success() {
        // Arrange
        when(productRepository.getMaxProductId()).thenReturn(5L);

        // Act
        String nextSku = productService.getNextSku();

        // Assert
        assertEquals("SKU006", nextSku);
    }
}
