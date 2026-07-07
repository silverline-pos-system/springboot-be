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
import com.silverline.erp.module.auth.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    private final UserProfileRepository userProfileRepository;
    private final ApprovalRepository approvalRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerUser(RegisterRequestDTO registerRequestDTO) {
        log.info("Registering user: username={}, email={}", registerRequestDTO.getUsername(), registerRequestDTO.getEmail());
        
        Optional<UserProfile> existUserByEmail = userProfileRepository.findByEmail(registerRequestDTO.getEmail());
        if (existUserByEmail.isPresent()) {
            log.warn("Business rule violation: registration failed because email '{}' already exists", registerRequestDTO.getEmail());
            return new RegisterResponseDTO("EMAIL: User with this email already exists");
        }
        
        Optional<UserProfile> existUserByUsername = userProfileRepository.findByUsername(registerRequestDTO.getUsername());
        if (existUserByUsername.isPresent()) {
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
        int attempts = 0;
        while (userProfileRepository.findByEmployeeId(employeeId).isPresent() && attempts < 100) {
            attempts++;
            Long maxNumber = userProfileRepository.findMaxEmployeeIdSequence();
            long nextNumber = (maxNumber != null ? maxNumber : 0) + 1 + attempts;
            employeeId = String.format("EMP-%03d", nextNumber);
        }
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

    private String generateSequentialEmployeeId() {
        Long maxNumber = userProfileRepository.findMaxEmployeeIdSequence();
        long nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("EMP-%03d", nextNumber);
    }
}
