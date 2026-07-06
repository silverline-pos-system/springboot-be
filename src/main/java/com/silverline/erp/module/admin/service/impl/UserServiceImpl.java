package com.silverline.erp.module.admin.service.impl;

import com.silverline.erp.common.audit.repository.ApprovalRepository;
import com.silverline.erp.domain.audit.Approval;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.infrastructure.email.EmailService;
import com.silverline.erp.module.admin.dto.UserDTO;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.service.UserService;
import com.silverline.erp.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ApprovalRepository approvalRepository;

    @Override
    public Long getAllUserCount() {
        return userRepository.count();
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllUsers();
        }
        String lowerQuery = query.toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(lowerQuery)) ||
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerQuery)) ||
                        (u.getEmployeeId() != null && u.getEmployeeId().toLowerCase().contains(lowerQuery)))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO registerManager(UserDTO userDTO) {
        // Basic validation
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        UserProfile user = new UserProfile();
        user.setFullName(userDTO.getFullName());
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        String employeeId = generateSequentialEmployeeId();
        int attempts = 0;
        while (userRepository.existsByEmployeeId(employeeId) && attempts < 100) {
            attempts++;
            Long maxNumber = userRepository.findMaxEmployeeIdSequence();
            long nextNumber = (maxNumber != null ? maxNumber : 0) + 1 + attempts;
            employeeId = String.format("EMP-%03d", nextNumber);
        }
        user.setEmployeeId(employeeId);

        Role role;
        try {
            role = userDTO.getRole() != null ? Role.valueOf(userDTO.getRole()) : Role.MANAGER;
        } catch (IllegalArgumentException e) {
            role = Role.MANAGER;
        }
        user.setRole(role);

        // NOTE: No branch assignment â€” managers are branch-free
        // Flow: Manager created by admin is PENDING by default
        user.setAccountStatus(AccountStatus.PENDING);

        // Generate a cryptographically secure temporary password
        String rawPassword = com.silverline.erp.common.security.SecurityUtils.generateSecureTemporaryPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));

        UserProfile savedUser = userRepository.save(user);


        return convertToDTO(savedUser);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long userId, UserDTO userDTO) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userDTO.getFullName() != null) user.setFullName(userDTO.getFullName());
        if (userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());

        // NOTE: No branch assignment update — branch_id removed from user_profiles and UserBranch table removed

        UserProfile updatedUser = userRepository.save(user);


        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        try {
            userRepository.deleteById(userId);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Cannot delete user: This user has associated records (e.g. Approvals) that prevent deletion. Please deactivate the user instead.");
        }
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean activating = user.getAccountStatus() != AccountStatus.ACTIVE;

        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            user.setAccountStatus(AccountStatus.SUSPENDED);
        } else {
            user.setAccountStatus(AccountStatus.ACTIVE);

            if (activating) {
                Long currentAdminId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
                if (currentAdminId != null) {
                    userRepository.findById(currentAdminId).ifPresent(user::setApprovedBy);
                    user.setApprovedAt(java.time.LocalDateTime.now());
                }
                try {
                    String tempPassword = com.silverline.erp.common.security.SecurityUtils.generateSecureTemporaryPassword();
                    user.setPassword(passwordEncoder.encode(tempPassword));
                    user.setMustChangePassword(true);

                    String subject = "Your Silverline Account Has Been Activated";
                    String htmlContent = com.silverline.erp.infrastructure.email.TemplateEngine.loadAndResolve(
                            "user_activation",
                            java.util.Map.of(
                                    "fullName", user.getFullName(),
                                    "username", user.getUsername(),
                                    "tempPassword", tempPassword
                            )
                    );

                    emailService.sendHtmlMessage(user.getEmail(), subject, htmlContent);
                } catch (Exception e) {
                    log.error("Failed to send activation email to user ID {}: {}", userId, e.getMessage(), e);
                }
            }
        }

        userRepository.save(user);
    }

    @Override
    public List<UserDTO> getManagers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MANAGER)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO convertToDTO(UserProfile user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : null);
        dto.setCreatedAt(user.getCreatedAt());

        // Set approved by information if user has been approved
        if (user.getApprovedBy() != null) {
            dto.setApprovedById(user.getApprovedBy().getUserId());
            dto.setApprovedByName(user.getApprovedBy().getFullName());
            dto.setApprovedAt(user.getApprovedAt());
        } else {
            // Fallback for historical records: read approver from approvals table
            Optional<Approval> latestApprovedRegistration = approvalRepository
                    .findByReferenceIdAndType(user.getUserId(), "USER_REGISTRATION")
                    .stream()
                    .filter(a -> "APPROVED".equalsIgnoreCase(a.getStatus()) && a.getApprovedBy() != null)
                    .max(Comparator.comparing(Approval::getApprovedAt, Comparator.nullsLast(Comparator.naturalOrder())));

            latestApprovedRegistration.ifPresent(a -> {
                dto.setApprovedById(a.getApprovedBy());
                userRepository.findById(a.getApprovedBy()).ifPresent(approver -> dto.setApprovedByName(approver.getFullName()));
                dto.setApprovedAt(a.getApprovedAt());
            });
        }

        // NOTE: No branch info — users are NOT tied to branches
        return dto;
    }

    private String generateSequentialEmployeeId() {
        Long maxNumber = userRepository.findMaxEmployeeIdSequence();
        long nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("EMP-%03d", nextNumber);
    }
}
