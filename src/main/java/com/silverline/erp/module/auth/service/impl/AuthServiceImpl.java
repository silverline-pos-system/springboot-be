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
import com.silverline.erp.module.auth.repo.BranchRepo;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.module.auth.service.AuthService;
import com.silverline.erp.module.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
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
public class AuthServiceImpl implements AuthService {

    private final UserProfileRepo userProfileRepo;
    private final BranchRepo branchRepo;
    private final ApprovalRepository approvalRepository;
    private final BranchRepository branchRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService activityLogService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfile findByEmail(String email) {
        Optional<UserProfile> userProfile = userProfileRepo.findByEmail(email);
        return userProfile.orElse(null);
    }

    @Override
    public UserProfile findByUsername(String username) {
        Optional<UserProfile> userProfile = userProfileRepo.findByUsername(username);
        return userProfile.orElse(null);
    }

    @Override
    public List<Branch> getAllBranches() {
        return branchRepo.findAll();
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerUser(RegisterRequestDTO registerRequestDTO) {
        UserProfile existUserByEmail = findByEmail(registerRequestDTO.getEmail());
        if (existUserByEmail != null) {
            return new RegisterResponseDTO("EMAIL: User with this email already exists");
        }
        UserProfile existUserByUsername = findByUsername(registerRequestDTO.getUsername());
        if (existUserByUsername != null) {
            return new RegisterResponseDTO("USERNAME: User with this username already exists");
        }

        Optional<UserProfile> existUserByPhone = userProfileRepo.findByPhone(registerRequestDTO.getPhone());
        if (existUserByPhone.isPresent()) {
            return new RegisterResponseDTO("PHONE: User with this phone number already exists");
        }

        UserProfile userProfile = new UserProfile();
        userProfile.setFullName(registerRequestDTO.getFullName());
        userProfile.setUsername(registerRequestDTO.getUsername());
        userProfile.setEmail(registerRequestDTO.getEmail());
        userProfile.setPassword(passwordEncoder.encode(registerRequestDTO.getPassword()));
        userProfile.setPhone(registerRequestDTO.getPhone());
        
        String employeeId = registerRequestDTO.getEmployeeId();
        if (employeeId != null && !employeeId.trim().isEmpty()) {
            Optional<UserProfile> existUserByEmployeeId = userProfileRepo.findByEmployeeId(employeeId);
            if (existUserByEmployeeId.isPresent()) {
                return new RegisterResponseDTO("EMPLOYEE_ID: User with this Employee ID already exists");
            }
        } else {
            employeeId = generateSequentialEmployeeId();
        }
        userProfile.setEmployeeId(employeeId);
        userProfile.setRole(null);
        userProfile.setAccountStatus(AccountStatus.PENDING);

        UserProfile registerUser = userProfileRepo.save(userProfile);

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
        UserProfile existUserByUsername = findByUsername(username);
        if (existUserByUsername == null) {
            return new LogInResponseDTO("Invalid username");
        }

        if (existUserByUsername.getAccountStatus() == AccountStatus.PENDING) {
            return new LogInResponseDTO("Account pending approval. Please wait for manager to activate your account.");
        }

        if (existUserByUsername.getAccountStatus() == AccountStatus.REJECTED) {
            return new LogInResponseDTO("Account has been rejected. Please contact administrator.");
        }

        if (existUserByUsername.getAccountStatus() == AccountStatus.SUSPENDED) {
            return new LogInResponseDTO("Account is suspended. Please contact administrator.");
        }

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(username, password));

        if (!authentication.isAuthenticated()) {
            return new LogInResponseDTO("Invalid credentials");
        }

        UserProfile user = userProfileRepo.findByUsername(username)
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
        Long maxNumber = userProfileRepo.findMaxEmployeeIdSequence();
        long nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("EMP%03d", nextNumber);
    }
}
