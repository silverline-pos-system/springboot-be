package com.silverline.erp.module.auth.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.auth.dto.LogInResponseDTO;
import com.silverline.erp.module.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditLogService activityLogService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userProfile = new UserProfile();
        userProfile.setUserId(1L);
        userProfile.setUsername("testuser");
        userProfile.setEmail("test@silverline.com");
        userProfile.setPhone("1234567890");
        userProfile.setAccountStatus(AccountStatus.ACTIVE);
        userProfile.setRole(Role.CASHIER);
    }

    @Test
    void logInUser_Success() {
        // Arrange
        when(userProfileRepository.findByUsername("testuser")).thenReturn(Optional.of(userProfile));
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);

        Branch branch = new Branch();
        branch.setBranchId(1L);
        branch.setIsActive(true);
        when(branchRepository.findAll()).thenReturn(Collections.singletonList(branch));
        when(jwtService.generateToken(anyMap(), eq(userProfile))).thenReturn("mockJwtToken");

        // Act
        LogInResponseDTO response = authService.logInUser("testuser", "securePassword");

        // Assert
        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("mockJwtToken", response.getToken());
        assertNotNull(userProfile.getLastLogin());
        verify(userProfileRepository, times(1)).save(userProfile);
    }

    @Test
    void logInUser_UserNotFound_ReturnsError() {
        // Arrange
        when(userProfileRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act
        LogInResponseDTO response = authService.logInUser("unknown", "password");

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertEquals("Invalid username", response.getMessage());
    }

    @Test
    void logInUser_PendingStatus_ReturnsError() {
        // Arrange
        userProfile.setAccountStatus(AccountStatus.PENDING);
        when(userProfileRepository.findByUsername("testuser")).thenReturn(Optional.of(userProfile));

        // Act
        LogInResponseDTO response = authService.logInUser("testuser", "password");

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("Account pending approval"));
    }
}
