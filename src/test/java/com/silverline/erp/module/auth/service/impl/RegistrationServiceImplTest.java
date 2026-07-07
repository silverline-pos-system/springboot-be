package com.silverline.erp.module.auth.service.impl;

import com.silverline.erp.common.audit.repository.ApprovalRepository;
import com.silverline.erp.domain.audit.Approval;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.auth.dto.RegisterRequestDTO;
import com.silverline.erp.module.auth.dto.RegisterResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private RegisterRequestDTO registerRequest;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@silverline.com");
        registerRequest.setFullName("Test User");
        registerRequest.setPhone("1234567890");
        registerRequest.setPassword("securePassword");

        userProfile = new UserProfile();
        userProfile.setUserId(1L);
        userProfile.setUsername("testuser");
        userProfile.setEmail("test@silverline.com");
        userProfile.setPhone("1234567890");
        userProfile.setAccountStatus(AccountStatus.PENDING);
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userProfileRepository.findByEmail("test@silverline.com")).thenReturn(Optional.empty());
        when(userProfileRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userProfileRepository.findByPhone("1234567890")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("securePassword")).thenReturn("hashedPassword");
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> {
            UserProfile u = i.getArgument(0);
            u.setUserId(10L);
            assertEquals("EMP-001", u.getEmployeeId());
            return u;
        });

        Branch branch = new Branch();
        branch.setBranchId(1L);
        branch.setIsActive(true);
        when(branchRepository.findAll()).thenReturn(Collections.singletonList(branch));

        // Act
        RegisterResponseDTO response = registrationService.registerUser(registerRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.getMessage().contains("registered successfully"));
        assertEquals("test@silverline.com", response.getEmail());
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(approvalRepository).save(any(Approval.class));
    }

    @Test
    void registerUser_DuplicateEmail_ReturnsError() {
        // Arrange
        when(userProfileRepository.findByEmail("test@silverline.com")).thenReturn(Optional.of(userProfile));

        // Act
        RegisterResponseDTO response = registrationService.registerUser(registerRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("EMAIL: User with this email already exists"));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }
}
