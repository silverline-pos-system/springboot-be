package com.nsbm.rocs.modules.manager.service;

import com.nsbm.rocs.entity.main.SecondaryRoleAssignment;
import com.nsbm.rocs.entity.main.UserProfile;
import com.nsbm.rocs.modules.manager.dto.AssignSecondaryRoleRequest;
import com.nsbm.rocs.modules.manager.dto.MySecondaryRoleResponse;
import com.nsbm.rocs.modules.manager.dto.SecondaryRoleAssignmentDTO;
import com.nsbm.rocs.modules.manager.repository.SecondaryRoleAssignmentRepository;
import com.nsbm.rocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.nsbm.rocs.common.exception.ResourceNotFoundException;
import com.nsbm.rocs.common.exception.DuplicateResourceException;
import com.nsbm.rocs.common.exception.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecondaryRoleService {

    private final SecondaryRoleAssignmentRepository assignmentRepo;
    private final UserRepository userRepo;

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "CASHIER", "SUPERVISOR", "STORE_KEEPER", "DTV_TECHNICIAN", "MOBILE_TECHNICIAN"
    );

    public List<SecondaryRoleAssignmentDTO> getAssignments(Long branchId) {
        List<SecondaryRoleAssignment> assignments = assignmentRepo
                .findByAssignedByBranchIdOrderByCreatedAtDesc(branchId);

        return assignments.stream().map(a -> {
            UserProfile user = userRepo.findById(a.getUserId()).orElse(null);
            SecondaryRoleAssignmentDTO dto = new SecondaryRoleAssignmentDTO();
            dto.setId(a.getId());
            dto.setUserId(a.getUserId());
            dto.setUsername(user != null ? user.getUsername() : "Unknown");
            dto.setPrimaryRole(user != null && user.getRole() != null ? user.getRole().name() : "Unknown");
            dto.setSecondaryRole(a.getSecondaryRole());
            dto.setExpiresAt(a.getExpiresAt().toString());
            dto.setReason(a.getReason());
            dto.setAssignedByBranchId(a.getAssignedByBranchId());
            dto.setCreatedAt(a.getCreatedAt().toString());
            dto.setRevoked(a.getRevoked());
            return dto;
        }).collect(Collectors.toList());
    }

    public SecondaryRoleAssignmentDTO assignRole(AssignSecondaryRoleRequest request) {
        // 1. Validate role
        if (!ALLOWED_ROLES.contains(request.getSecondaryRole())) {
            throw new ValidationException("Invalid secondary role");
        }

        // 2. Validate user exists
        UserProfile user = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. Cannot assign same as primary
        if (user.getRole() != null && user.getRole().name().equalsIgnoreCase(request.getSecondaryRole())) {
            throw new ValidationException("Cannot assign same role as primary");
        }

        // 4. Parse and validate expiry
        LocalDateTime expiresAt = LocalDateTime.parse(
                request.getExpiresAt(), DateTimeFormatter.ISO_DATE_TIME
        );
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Expiry must be in the future");
        }

        // 5. Check no active assignment exists
        if (assignmentRepo.existsByUserIdAndRevokedFalseAndExpiresAtAfter(
                request.getUserId(), LocalDateTime.now())) {
            throw new DuplicateResourceException("User already has an active secondary role");
        }

        // 6. Create assignment
        SecondaryRoleAssignment assignment = new SecondaryRoleAssignment();
        assignment.setUserId(request.getUserId());
        assignment.setSecondaryRole(request.getSecondaryRole());
        assignment.setExpiresAt(expiresAt);
        assignment.setReason(request.getReason());
        assignment.setAssignedByBranchId(request.getAssignedByBranchId());

        SecondaryRoleAssignment saved = assignmentRepo.save(assignment);

        // 7. Return DTO
        SecondaryRoleAssignmentDTO dto = new SecondaryRoleAssignmentDTO();
        dto.setId(saved.getId());
        dto.setUserId(saved.getUserId());
        dto.setSecondaryRole(saved.getSecondaryRole());
        dto.setExpiresAt(saved.getExpiresAt().toString());
        dto.setReason(saved.getReason());
        dto.setAssignedByBranchId(saved.getAssignedByBranchId());
        dto.setCreatedAt(saved.getCreatedAt().toString());
        return dto;
    }

    public void revokeRole(Long id) {
        SecondaryRoleAssignment assignment = assignmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (assignment.getRevoked()) {
            throw new ValidationException("Assignment already revoked");
        }

        assignment.setRevoked(true);
        assignment.setRevokedAt(LocalDateTime.now());
        assignmentRepo.save(assignment);
    }

    public MySecondaryRoleResponse getMySecondaryRole(UserDetails userDetails) {
        // Resolve userId from UserDetails
        UserProfile user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<SecondaryRoleAssignment> active = assignmentRepo
                .findFirstByUserIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        user.getUserId(), LocalDateTime.now()
                );

        if (active.isEmpty()) {
            return null; // Controller returns 404
        }

        SecondaryRoleAssignment a = active.get();
        MySecondaryRoleResponse response = new MySecondaryRoleResponse();
        response.setSecondaryRole(a.getSecondaryRole());
        response.setExpiresAt(a.getExpiresAt().toString());
        response.setReason(a.getReason());
        return response;
    }
}


