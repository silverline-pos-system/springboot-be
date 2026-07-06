package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.event.SaleCompletedEvent;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.admin.service.SaasFeatureService;
import com.silverline.erp.module.inventory.service.BatchService;
import com.silverline.erp.module.inventory.service.ProductSerialService;
import com.silverline.erp.module.inventory.service.ProductService;
import com.silverline.erp.module.inventory.service.StockService;
import com.silverline.erp.module.pos.dto.sale.CreateSaleRequest;
import com.silverline.erp.module.pos.dto.sale.PaymentRequest;
import com.silverline.erp.module.pos.dto.sale.SaleItemRequest;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.repository.PaymentRepository;
import com.silverline.erp.module.pos.repository.SaleItemRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.module.pos.service.SaleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PosSaleServiceImplTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private SaleItemRepository saleItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ProductService productService;
    @Mock
    private StockService stockService;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BatchService batchService;
    @Mock
    private SaasFeatureService featureService;
    @Mock
    private ProductSerialService productSerialService;
    @Mock
    private SaleQueryService saleQueryService;

    @InjectMocks
    private PosSaleServiceImpl posSaleService;

    private CreateSaleRequest request;
    private Long branchId = 1L;
    private Long cashierId = 2L;
    private Long shiftId = 3L;

    @BeforeEach
    void setUp() {
        request = new CreateSaleRequest();
        request.setCustomerId(10L);
        request.setDiscount(BigDecimal.TEN);
        request.setNotes("Test Sale Notes");
        request.setSaleType("RETAIL");
        request.setStatus("PAID");

        SaleItemRequest itemReq = new SaleItemRequest();
        itemReq.setProductId(100L);
        itemReq.setQuantity(BigDecimal.valueOf(2));
        itemReq.setUnitPrice(BigDecimal.valueOf(50));
        itemReq.setDiscount(BigDecimal.ZERO);
        request.setItems(Collections.singletonList(itemReq));

        PaymentRequest pr = new PaymentRequest();
        pr.setPaymentType("CASH");
        pr.setAmount(BigDecimal.valueOf(90));
        request.setPayments(Collections.singletonList(pr));
    }

    @Test
    void createSale_Success() {
        // Arrange
        Product mockProduct = new Product();
        mockProduct.setProductId(100L);
        mockProduct.setName("Test Product");
        mockProduct.setSellingPrice(BigDecimal.valueOf(50));

        UserProfile mockUser = new UserProfile();
        mockUser.setUsername("testCashier");

        when(featureService.isFeatureEnabled("ALLOW_OUT_OF_STOCK")).thenReturn(false);
        when(productService.findById(100L)).thenReturn(mockProduct);
        when(stockService.getCurrentStock(branchId, 100L)).thenReturn(5);
        when(saleRepository.save(any(Sale.class))).thenReturn(1L);
        when(userProfileRepository.findById(cashierId)).thenReturn(Optional.of(mockUser));

        SaleResponse expectedResponse = new SaleResponse.Builder()
                .saleId(1L)
                .invoiceNo("INV-20260702-00001")
                .netTotal(BigDecimal.valueOf(90))
                .build();

        when(saleQueryService.mapToResponse(any(Sale.class), anyList(), anyList())).thenReturn(expectedResponse);

        // Act
        SaleResponse actualResponse = posSaleService.createSale(request, branchId, cashierId, shiftId);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(1L, actualResponse.getSaleId());
        assertEquals(BigDecimal.valueOf(90), actualResponse.getNetTotal());

        verify(stockService).reduceStock(eq(branchId), eq(100L), eq(2));
        verify(eventPublisher).publishEvent(any(SaleCompletedEvent.class));
    }

    @Test
    void createSale_OutOfStock_ThrowsException() {
        // Arrange
        Product mockProduct = new Product();
        mockProduct.setProductId(100L);
        mockProduct.setName("Test Product");

        when(featureService.isFeatureEnabled("ALLOW_OUT_OF_STOCK")).thenReturn(false);
        when(productService.findById(100L)).thenReturn(mockProduct);
        when(stockService.getCurrentStock(branchId, 100L)).thenReturn(0);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                posSaleService.createSale(request, branchId, cashierId, shiftId)
        );

        assertTrue(exception.getMessage().contains("out of stock"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void createSale_InsufficientStock_ThrowsException() {
        // Arrange
        Product mockProduct = new Product();
        mockProduct.setProductId(100L);
        mockProduct.setName("Test Product");

        when(featureService.isFeatureEnabled("ALLOW_OUT_OF_STOCK")).thenReturn(false);
        when(productService.findById(100L)).thenReturn(mockProduct);
        when(stockService.getCurrentStock(branchId, 100L)).thenReturn(1); // request wants 2

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                posSaleService.createSale(request, branchId, cashierId, shiftId)
        );

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void createSale_BypassStockValidationWhenAllowOutOfStockActive() {
        // Arrange
        Product mockProduct = new Product();
        mockProduct.setProductId(100L);
        mockProduct.setName("Test Product");

        UserProfile mockUser = new UserProfile();
        mockUser.setUsername("testCashier");

        when(featureService.isFeatureEnabled("ALLOW_OUT_OF_STOCK")).thenReturn(true);
        when(saleRepository.save(any(Sale.class))).thenReturn(1L);
        when(userProfileRepository.findById(cashierId)).thenReturn(Optional.of(mockUser));

        SaleResponse expectedResponse = new SaleResponse.Builder()
                .saleId(1L)
                .build();

        when(saleQueryService.mapToResponse(any(Sale.class), anyList(), anyList())).thenReturn(expectedResponse);

        // Act
        SaleResponse actualResponse = posSaleService.createSale(request, branchId, cashierId, shiftId);

        // Assert
        assertNotNull(actualResponse);
        verify(stockService, never()).getCurrentStock(anyLong(), anyLong());
        verify(stockService).reduceStock(eq(branchId), eq(100L), eq(2));
    }

    @Test
    void updateSaleStatus_Success() {
        // Arrange
        Sale mockSale = new Sale();
        mockSale.setSaleId(1L);
        mockSale.setPaymentStatus("PENDING");

        when(saleRepository.findById(1L)).thenReturn(Optional.of(mockSale));

        // Act
        posSaleService.updateSaleStatus(1L, "PAID");

        // Assert
        assertEquals("PAID", mockSale.getPaymentStatus());
        verify(saleRepository).save(mockSale);
    }
}
