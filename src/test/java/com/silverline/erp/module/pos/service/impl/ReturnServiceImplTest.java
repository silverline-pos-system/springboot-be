package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.pos.SalesReturn;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.inventory.service.StockService;
import com.silverline.erp.module.pos.dto.returns.ReturnRequest;
import com.silverline.erp.module.pos.repository.SalesReturnItemRepository;
import com.silverline.erp.module.pos.repository.SalesReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceImplTest {

    @Mock
    private SalesReturnRepository salesReturnRepository;
    @Mock
    private SalesReturnItemRepository salesReturnItemRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private StockService stockService;
    @Mock
    private AuditLogService activityLogService;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private ReturnRequest request;
    private ReturnRequest.ReturnItemRequest itemRequest;

    @BeforeEach
    void setUp() {
        request = new ReturnRequest();
        request.setSaleId(10L);
        request.setBranchId(1L);
        request.setReason("Defective");
        request.setRefundMethod("CASH");
        request.setSupervisorUsername("adminUser");
        request.setSupervisorPassword("adminPass");

        itemRequest = new ReturnRequest.ReturnItemRequest();
        itemRequest.setSaleItemId(101L);
        itemRequest.setProductId(201L);
        itemRequest.setQty(BigDecimal.valueOf(1));
        itemRequest.setUnitPrice(BigDecimal.valueOf(100));

        request.setItems(Collections.singletonList(itemRequest));
    }

    @Test
    void processReturn_Success() {
        // Arrange
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);

        UserProfile supervisor = new UserProfile();
        supervisor.setUserId(2L);
        supervisor.setUsername("adminUser");
        supervisor.setRole(Role.SUPER_ADMIN);
        when(userProfileRepository.findByUsername("adminUser")).thenReturn(Optional.of(supervisor));

        SalesReturn mockReturn = new SalesReturn();
        mockReturn.setReturnId(50L);
        when(salesReturnRepository.save(any(SalesReturn.class))).thenReturn(mockReturn);

        // Act
        Long returnId = returnService.processReturn(request);

        // Assert
        assertEquals(50L, returnId);
        verify(stockService).increaseStock(eq(1L), eq(201L), eq(1));
        verify(salesReturnItemRepository).saveAll(anyList());
    }

    @Test
    void processReturn_NoSupervisorCredentials_ThrowsException() {
        // Arrange
        request.setSupervisorUsername("");
        request.setSupervisorPassword("");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                returnService.processReturn(request)
        );
        assertTrue(exception.getMessage().contains("Supervisor approval is required"));
        verify(salesReturnRepository, never()).save(any(SalesReturn.class));
    }

    @Test
    void processReturn_NonSupervisorRole_ThrowsException() {
        // Arrange
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);

        UserProfile nonSupervisor = new UserProfile();
        nonSupervisor.setUserId(3L);
        nonSupervisor.setUsername("cashierUser");
        nonSupervisor.setRole(Role.CASHIER); // not ADMIN/MANAGER/SUPERVISOR/SUPER_ADMIN
        when(userProfileRepository.findByUsername("adminUser")).thenReturn(Optional.of(nonSupervisor));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                returnService.processReturn(request)
        );
        assertTrue(exception.getMessage().contains("supervisor privileges"));
        verify(salesReturnRepository, never()).save(any(SalesReturn.class));
    }

    @Test
    void processReturn_AuthenticationFailed_ThrowsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                returnService.processReturn(request)
        );
        assertTrue(exception.getMessage().contains("Supervisor authorization failed"));
        verify(salesReturnRepository, never()).save(any(SalesReturn.class));
    }
}
