package com.silverline.erp.module.auth.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.dto.BranchDTO;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.auth.dto.LogInResponseDTO;
import com.silverline.erp.module.auth.service.AuthService;
import com.silverline.erp.module.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserProfileRepository userProfileRepository;
    private final BranchRepository branchRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService activityLogService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfile findByEmail(String email) {
        Optional<UserProfile> userProfile = userProfileRepository.findByEmail(email);
        return userProfile.orElse(null);
    }

    @Override
    public UserProfile findByUsername(String username) {
        Optional<UserProfile> userProfile = userProfileRepository.findByUsername(username);
        return userProfile.orElse(null);
    }

    @Override
    public LogInResponseDTO logInUser(String username, String password) {
        log.info("Logging in user: username={}", username);
        UserProfile existUserByUsername = findByUsername(username);
        if (existUserByUsername == null) {
            log.warn("Business rule violation: login failed because user '{}' does not exist", username);
            return new LogInResponseDTO("Invalid username");
        }

        if (existUserByUsername.getAccountStatus() == AccountStatus.PENDING) {
            log.warn("Business rule violation: login failed because user '{}' is PENDING approval", username);
            return new LogInResponseDTO("Account pending approval. Please wait for manager to activate your account.");
        }

        if (existUserByUsername.getAccountStatus() == AccountStatus.REJECTED) {
            log.warn("Business rule violation: login failed because user '{}' is REJECTED", username);
            return new LogInResponseDTO("Account has been rejected. Please contact administrator.");
        }

        if (existUserByUsername.getAccountStatus() == AccountStatus.SUSPENDED) {
            log.warn("Business rule violation: login failed because user '{}' is SUSPENDED", username);
            return new LogInResponseDTO("Account is suspended. Please contact administrator.");
        }

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(username, password));

        if (!authentication.isAuthenticated()) {
            return new LogInResponseDTO("Invalid credentials");
        }

        UserProfile user = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastLogin(java.time.LocalDateTime.now());
        userProfileRepository.save(user);

        List<Branch> allBranches = branchRepository.findAll().stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .collect(Collectors.toList());
        List<BranchDTO> branchDtos = allBranches.stream()
                .map(b -> BranchDTO.builder()
                        .branchId(b.getBranchId())
                        .name(b.getName())
                        .code(b.getCode())
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> extraClaims = new HashMap<>();
        String roleName = user.getRole() != null ? user.getRole().name() : "PENDING";
        extraClaims.put("role", roleName);
        extraClaims.put("branches", branchDtos);

        String jwtToken = jwtService.generateToken(extraClaims, user);

        LogInResponseDTO response = new LogInResponseDTO(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                jwtToken,
                user.getRole(),
                null,
                null,
                "Login successful"
        );
        response.setBranches(branchDtos);

        activityLogService.logActivity(
                allBranches.isEmpty() ? 1L : allBranches.get(0).getBranchId(),
                user.getUserId(),
                user.getUsername(),
                roleName,
                "LOGIN",
                "USER",
                user.getUserId(),
                "User logged in: " + user.getUsername(),
                "{}"
        );

        return response;
    }

    @Override
    public boolean verifySupervisor(String username, String password) {
        UserProfile user = findByUsername(username);
        if (user == null || user.getAccountStatus() != AccountStatus.ACTIVE || user.getRole() == null) {
            return false;
        }

        String roleName = user.getRole().name();
        boolean isSupervisor = roleName.equals("SUPER_ADMIN") ||
                roleName.equals("ADMIN") ||
                roleName.equals("MANAGER") ||
                roleName.equals("SUPERVISOR");

        if (!isSupervisor) return false;
        return passwordEncoder.matches(password, user.getPassword());
    }
}
