package com.silverline.erp.integration;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.dto.LogInResponseDTO;
import com.silverline.erp.module.auth.dto.RegisterRequestDTO;
import com.silverline.erp.module.auth.dto.RegisterResponseDTO;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    public void testAuthFlow_Success() {
        // 1. Create and persist an active Branch so login can find active branches
        Branch branch = new Branch();
        branch.setName("Auth Test Branch");
        branch.setCode("BR_AUTH_001");
        branch.setIsActive(true);
        branch = branchRepository.save(branch);

        // 2. Prepare RegisterRequestDTO
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setFullName("Auth Test User");
        registerRequest.setUsername("authUserTest");
        registerRequest.setEmail("authUserTest@example.com");
        registerRequest.setPassword("securePassword123");
        registerRequest.setPhone("+94771234567");
        registerRequest.setEmployeeId("EMP_AUTH_001");
        registerRequest.setBranchId(branch.getBranchId());

        // 3. Register user
        RegisterResponseDTO registerResponse = authService.registerUser(registerRequest);
        assertNotNull(registerResponse);
        assertNotNull(registerResponse.getUserId());
        assertEquals("authUserTest@example.com", registerResponse.getEmail());
        assertEquals(AccountStatus.PENDING, registerResponse.getAccountStatus());
        assertTrue(registerResponse.getMessage().contains("Pending Manager approval"));

        // 4. Try log in while PENDING (should fail)
        LogInResponseDTO pendingLoginResponse = authService.logInUser("authUserTest", "securePassword123");
        assertNotNull(pendingLoginResponse);
        assertTrue(pendingLoginResponse.getMessage().contains("Account pending approval"));
        assertNull(pendingLoginResponse.getToken());

        // 5. Approve the user manually
        UserProfile registeredUser = userProfileRepository.findById(registerResponse.getUserId()).orElseThrow();
        registeredUser.setAccountStatus(AccountStatus.ACTIVE);
        registeredUser.setRole(Role.CASHIER);
        userProfileRepository.save(registeredUser);

        // 6. Log in with correct credentials
        LogInResponseDTO loginResponse = authService.logInUser("authUserTest", "securePassword123");
        assertNotNull(loginResponse);
        assertEquals("Login successful", loginResponse.getMessage());
        assertNotNull(loginResponse.getToken());
        assertEquals(Role.CASHIER, loginResponse.getRole());
        assertNotNull(loginResponse.getBranches());
        assertFalse(loginResponse.getBranches().isEmpty());
        assertTrue(loginResponse.getBranches().stream().anyMatch(b -> "BR_AUTH_001".equals(b.getCode())));
    }
}
