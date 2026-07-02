package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.common.event.ShiftClosedEvent;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.pos.CashShift;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.module.pos.dto.ShiftStartRequest;
import com.silverline.erp.module.pos.dto.shift.CloseShiftRequest;
import com.silverline.erp.module.pos.repository.CashFlowRepository;
import com.silverline.erp.module.pos.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceImplTest {

    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserProfileRepo userProfileRepo;
    @Mock
    private CashFlowRepository cashFlowRepository;
    @Mock
    private AuditLogService activityLogService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ShiftServiceImpl shiftService;

    private ShiftStartRequest startRequest;
    private CloseShiftRequest closeRequest;

    @BeforeEach
    void setUp() {
        startRequest = new ShiftStartRequest();
        startRequest.setCashierId(10L);
        startRequest.setBranchId(1L);
        startRequest.setOpeningCash(BigDecimal.valueOf(100));

        closeRequest = new CloseShiftRequest();
        closeRequest.setClosingCash(BigDecimal.valueOf(500));
        closeRequest.setNotes("Shift Close Notes");
    }

    @Test
    void startShift_Success_NoSupervisor() {
        // Arrange
        when(shiftRepository.hasOpenShift(10L)).thenReturn(false);
        when(shiftRepository.save(any(CashShift.class))).thenReturn(100L);
        when(userProfileRepo.findById(10L)).thenReturn(Optional.empty());

        // Act
        Long shiftId = shiftService.startShift(startRequest);

        // Assert
        assertEquals(100L, shiftId);
        verify(shiftRepository).save(any(CashShift.class));
        verify(activityLogService).logActivity(anyLong(), any(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void startShift_Success_WithSupervisor() {
        // Arrange
        startRequest.setSupervisorUsername("admin");
        startRequest.setSupervisorPassword("adminPass");

        UserProfile supervisor = new UserProfile();
        supervisor.setUserId(2L);
        supervisor.setUsername("admin");
        supervisor.setAccountStatus(AccountStatus.ACTIVE);
        supervisor.setRole(Role.SUPER_ADMIN);

        when(userProfileRepo.findByUsername("admin")).thenReturn(Optional.of(supervisor));
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);

        when(shiftRepository.hasOpenShift(10L)).thenReturn(false);
        when(shiftRepository.save(any(CashShift.class))).thenReturn(100L);
        when(userProfileRepo.findById(10L)).thenReturn(Optional.empty());

        // Act
        Long shiftId = shiftService.startShift(startRequest);

        // Assert
        assertEquals(100L, shiftId);
        verify(shiftRepository).save(any(CashShift.class));
    }

    @Test
    void startShift_CashierHasOpenShift_ThrowsException() {
        // Arrange
        when(shiftRepository.hasOpenShift(10L)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shiftService.startShift(startRequest)
        );
        assertTrue(exception.getMessage().contains("already has an active shift"));
        verify(shiftRepository, never()).save(any(CashShift.class));
    }

    @Test
    void getActiveShiftId_Success() {
        // Arrange
        CashShift shift = new CashShift();
        shift.setShiftId(100L);
        when(shiftRepository.findOpenShiftByCashierId(10L)).thenReturn(Optional.of(shift));

        // Act
        Long activeShiftId = shiftService.getActiveShiftId(10L);

        // Assert
        assertEquals(100L, activeShiftId);
    }

    @Test
    void getActiveShiftId_NoActiveShift_ThrowsException() {
        // Arrange
        when(shiftRepository.findOpenShiftByCashierId(10L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shiftService.getActiveShiftId(10L)
        );
        assertTrue(exception.getMessage().contains("No active shift found"));
    }

    @Test
    void closeShift_Success() {
        // Arrange
        CashShift shift = new CashShift();
        shift.setShiftId(100L);
        shift.setCashierId(10L);
        shift.setOpeningCash(BigDecimal.valueOf(100));
        shift.setTotalSales(BigDecimal.valueOf(400));
        shift.setTotalReturns(BigDecimal.valueOf(50));
        shift.setStatus(CashShift.ShiftStatus.OPEN);

        when(shiftRepository.findOpenShiftByCashierId(10L)).thenReturn(Optional.of(shift));
        when(cashFlowRepository.countByShiftIdAndStatus(100L, "PENDING")).thenReturn(0L);
        when(shiftRepository.findByIdWithStats(100L)).thenReturn(Optional.of(shift));
        when(userProfileRepo.findById(10L)).thenReturn(Optional.empty());

        // Act
        shiftService.closeShift(10L, closeRequest);

        // Assert
        assertEquals(CashShift.ShiftStatus.CLOSED, shift.getStatus());
        assertEquals(BigDecimal.valueOf(500), shift.getClosingCash());
        assertEquals(BigDecimal.valueOf(450), shift.getExpectedCash());
        assertEquals(BigDecimal.valueOf(50), shift.getCashDifference());

        verify(shiftRepository).update(shift);
        verify(eventPublisher).publishEvent(any(ShiftClosedEvent.class));
    }

    @Test
    void closeShift_PendingCashFlow_ThrowsException() {
        // Arrange
        CashShift shift = new CashShift();
        shift.setShiftId(100L);
        when(shiftRepository.findOpenShiftByCashierId(10L)).thenReturn(Optional.of(shift));
        when(cashFlowRepository.countByShiftIdAndStatus(100L, "PENDING")).thenReturn(2L);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                shiftService.closeShift(10L, closeRequest)
        );
        assertTrue(exception.getMessage().contains("pending cash flow requests"));
        verify(shiftRepository, never()).update(any(CashShift.class));
    }
}
