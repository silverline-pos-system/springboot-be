package com.silverline.erp.module.auth.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.common.audit.repository.ApprovalRepository;
import com.silverline.erp.common.audit.repository.PasswordResetRequestRepository;
import com.silverline.erp.domain.audit.Approval;
import com.silverline.erp.domain.audit.PasswordResetRequest;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.dto.BranchDTO;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.dto.LogInResponseDTO;
import com.silverline.erp.module.auth.dto.RegisterRequestDTO;
import com.silverline.erp.module.auth.dto.RegisterResponseDTO;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.auth.service.AuthService;
import com.silverline.erp.module.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ApprovalRepository approvalRepository;
    private final BranchRepository branchRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
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
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerUser(RegisterRequestDTO registerRequestDTO) {
        log.info("Registering user: username={}, email={}", registerRequestDTO.getUsername(), registerRequestDTO.getEmail());
        UserProfile existUserByEmail = findByEmail(registerRequestDTO.getEmail());
        if (existUserByEmail != null) {
            log.warn("Business rule violation: registration failed because email '{}' already exists", registerRequestDTO.getEmail());
            return new RegisterResponseDTO("EMAIL: User with this email already exists");
        }
        UserProfile existUserByUsername = findByUsername(registerRequestDTO.getUsername());
        if (existUserByUsername != null) {
            log.warn("Business rule violation: registration failed because username '{}' already exists", registerRequestDTO.getUsername());
            return new RegisterResponseDTO("USERNAME: User with this username already exists");
        }

        Optional<UserProfile> existUserByPhone = userProfileRepository.findByPhone(registerRequestDTO.getPhone());
        if (existUserByPhone.isPresent()) {
            log.warn("Business rule violation: registration failed because phone '{}' already exists", registerRequestDTO.getPhone());
            return new RegisterResponseDTO("PHONE: User with this phone number already exists");
        }

        UserProfile userProfile = new UserProfile();
        userProfile.setFullName(registerRequestDTO.getFullName());
        userProfile.setUsername(registerRequestDTO.getUsername());
        userProfile.setEmail(registerRequestDTO.getEmail());
        userProfile.setPassword(passwordEncoder.encode(registerRequestDTO.getPassword()));
        userProfile.setPhone(registerRequestDTO.getPhone());
        
        String employeeId = generateSequentialEmployeeId();
        userProfile.setEmployeeId(employeeId);
        userProfile.setRole(null);
        userProfile.setAccountStatus(AccountStatus.PENDING);

        UserProfile registerUser = userProfileRepository.save(userProfile);

        Approval approval = new Approval();
        List<Branch> activeBranches = branchRepository.findAll().stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .collect(Collectors.toList());
        if (!activeBranches.isEmpty()) {
            approval.setBranchId(activeBranches.get(0).getBranchId());
        } else {
            approval.setBranchId(1L);
        }
        approval.setType("USER_REGISTRATION");
        approval.setReferenceId(registerUser.getUserId());
        approval.setReferenceNo("USER-" + registerUser.getUsername());
        approval.setStatus("PENDING");
        approval.setRequestedBy(registerUser.getUserId());
        approval.setRequestNotes("User registration for " + registerUser.getFullName());
        approvalRepository.save(approval);

        return new RegisterResponseDTO(
                registerUser.getUserId(),
                registerUser.getEmail(),
                registerUser.getFullName(),
                registerUser.getRole(),
                registerUser.getAccountStatus(),
                "User registered successfully. Pending Manager approval."
        );
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

    @Override
    @Transactional
    public void forgotPassword(String username, String newPassword, String reason) {
        UserProfile user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Username not found. Please check and try again.");
        }

        List<PasswordResetRequest> existing = passwordResetRequestRepository.findByUserId(user.getUserId());
        boolean hasPending = existing.stream().anyMatch(r -> "PENDING".equals(r.getStatus()));
        if (hasPending) {
            throw new RuntimeException("You already have a pending password reset request. Please wait for admin approval.");
        }

        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setUserId(user.getUserId());
        resetRequest.setUsername(user.getUsername());
        resetRequest.setFullName(user.getFullName());
        resetRequest.setEmail(user.getEmail());
        resetRequest.setNewPasswordHash(passwordEncoder.encode(newPassword));
        resetRequest.setStatus("PENDING");
        resetRequest.setRequestNotes(reason != null ? reason : "Password reset requested");

        passwordResetRequestRepository.save(resetRequest);
    }

    private String generateSequentialEmployeeId() {
        Long maxNumber = userProfileRepository.findMaxEmployeeIdSequence();
        long nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("EMP-%03d", nextNumber);
    }
}
