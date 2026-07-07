package com.silverline.erp.module.manager.service.impl;

import com.silverline.erp.common.audit.repository.ApprovalRepository;
import com.silverline.erp.common.audit.repository.UserActivityLogRepository;
import com.silverline.erp.domain.audit.Approval;
import com.silverline.erp.domain.audit.UserActivityLog;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.pos.CashFlow;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.infrastructure.email.EmailService;
import com.silverline.erp.module.analytics.dto.StaffSummaryDTO;
import com.silverline.erp.module.manager.dto.ActivityLogDTO;
import com.silverline.erp.module.manager.dto.ApprovalDTO;
import com.silverline.erp.module.manager.repository.ManagerUserRepository;
import com.silverline.erp.module.manager.service.ManagerService;
import com.silverline.erp.module.pos.service.CashReconciliationService;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.domain.branch.Branch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final ManagerUserRepository userRepository;
    private final ApprovalRepository approvalRepository;
    private final UserActivityLogRepository activityLogRepository;
    private final CashReconciliationService cashReconciliationService;
    private final EmailService emailService;
    private final BranchRepository branchRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ===== STAFF SUMMARY =====

    @Override
    public List<StaffSummaryDTO> getStaffSummary(Long branchId) {
        List<UserProfile> users;
        if (branchId != null) {
            users = userRepository.findActiveByBranchId(branchId);
        } else {
            users = userRepository.findAllActive();
        }

        return users.stream()
                .filter(u -> u.getRole() == null || (!u.getRole().name().equals("SUPER_ADMIN") && !u.getRole().name().equals("MANAGER")))
                .map(user -> StaffSummaryDTO.builder()
                        .userId(user.getUserId())
                        .name(user.getFullName())
                        .role(user.getRole() != null ? user.getRole().name() : "N/A")
                        .lastLogin(formatLastLogin(user.getLastLogin()))
                        .status(determineUserStatus(user))
                        .approvedBy(user.getApprovedBy() != null ? user.getApprovedBy().getFullName() : "System")
                        .approvedAt(user.getApprovedAt() != null ? user.getApprovedAt().format(DATE_TIME_FORMATTER) : null)
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .employeeId(user.getEmployeeId())
                        .build())
                .collect(Collectors.toList());
    }

    // ===== APPROVALS =====

    @Override
    public List<ApprovalDTO> getMyApprovals(Long branchId) {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) return new ArrayList<>();

        List<Approval> approvals;
        if (branchId != null) {
            approvals = approvalRepository.findByBranchId(branchId);
        } else {
            approvals = approvalRepository.findAll();
        }

        java.util.Set<Long> userIds = new java.util.HashSet<>();
        java.util.Set<Long> branchIds = new java.util.HashSet<>();
        approvals.forEach(a -> {
            if (a.getRequestedBy() != null) userIds.add(a.getRequestedBy());
            if (a.getApprovedBy() != null) userIds.add(a.getApprovedBy());
            if (a.getBranchId() != null) branchIds.add(a.getBranchId());
        });

        Map<Long, UserProfile> userMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(UserProfile::getUserId, u -> u));
        }

        Map<Long, String> branchMap = new java.util.HashMap<>();
        if (!branchIds.isEmpty()) {
            branchMap = branchRepository.findAllById(branchIds).stream()
                    .collect(Collectors.toMap(Branch::getBranchId, Branch::getName));
        }

        final Map<Long, UserProfile> finalUserMap = userMap;
        final Map<Long, String> finalBranchMap = branchMap;
        return approvals.stream()
                .map(approval -> mapToApprovalDTO(
                        approval,
                        finalUserMap.get(approval.getRequestedBy()),
                        finalUserMap.get(approval.getApprovedBy()),
                        finalBranchMap.getOrDefault(approval.getBranchId(), "-")
                ))
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalDTO> getApprovals(String status, Long branchId) {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) return new ArrayList<>();

        List<Approval> approvals;
        if (branchId != null) {
            if (status != null && !status.isEmpty()) {
                approvals = approvalRepository.findByBranchIdAndStatus(branchId, status.toUpperCase());
            } else {
                approvals = approvalRepository.findByBranchId(branchId);
            }
        } else {
            if (status != null && !status.isEmpty()) {
                approvals = approvalRepository.findByStatus(status.toUpperCase());
            } else {
                approvals = approvalRepository.findAll();
            }
        }

        java.util.Set<Long> userIds = new java.util.HashSet<>();
        java.util.Set<Long> branchIds = new java.util.HashSet<>();
        approvals.forEach(a -> {
            if (a.getRequestedBy() != null) userIds.add(a.getRequestedBy());
            if (a.getApprovedBy() != null) userIds.add(a.getApprovedBy());
            if (a.getBranchId() != null) branchIds.add(a.getBranchId());
        });

        Map<Long, UserProfile> userMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(UserProfile::getUserId, u -> u));
        }

        Map<Long, String> branchMap = new java.util.HashMap<>();
        if (!branchIds.isEmpty()) {
            branchMap = branchRepository.findAllById(branchIds).stream()
                    .collect(Collectors.toMap(Branch::getBranchId, Branch::getName));
        }

        final Map<Long, UserProfile> finalUserMap = userMap;
        final Map<Long, String> finalBranchMap = branchMap;
        return approvals.stream()
                .map(approval -> mapToApprovalDTO(
                        approval,
                        finalUserMap.get(approval.getRequestedBy()),
                        finalUserMap.get(approval.getApprovedBy()),
                        finalBranchMap.getOrDefault(approval.getBranchId(), "-")
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApprovalDTO updateApprovalStatus(Long approvalId, String status, String notes, String role) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("Approval not found: " + approvalId));

        Long approverId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (approverId == null) {
            throw new com.silverline.erp.common.exception.UnauthorizedException("Authenticated user required to approve requests");
        }

        approval.setStatus(status.toUpperCase());
        approval.setApprovalNotes(notes);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setApprovedBy(approverId);

        UserProfile user = null;
        if ("USER_REGISTRATION".equals(approval.getType()) && "APPROVED".equalsIgnoreCase(status)) {
            user = userRepository.findById(approval.getReferenceId())
                    .orElseThrow(() -> new RuntimeException("Ref User not found: " + approval.getReferenceId()));

            user.setAccountStatus(AccountStatus.ACTIVE);
            userRepository.findById(approverId).ifPresent(user::setApprovedBy);
            user.setApprovedAt(LocalDateTime.now());
            if (role != null && !role.trim().isEmpty()) {
                if (role.toUpperCase().equals("SUPER_ADMIN") || role.toUpperCase().equals("MANAGER")) {
                    throw new RuntimeException("Permission denied: Cannot assign Super Admin or Manager roles from this console.");
                }
                try {
                    user.setRole(Role.valueOf(role));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid role: " + role);
                }
            }
            userRepository.save(user);

            try {
                String assignedRole = user.getRole() != null ? user.getRole().name() : "Staff";
                String subject = "Welcome to ROCS - Your Account Has Been Approved!";
                String htmlContent = com.silverline.erp.infrastructure.email.TemplateEngine.loadAndResolve(
                        "registration_approved",
                        Map.of(
                                "fullName", user.getFullName(),
                                "username", user.getUsername(),
                                "role", assignedRole
                        )
                );

                emailService.sendHtmlMessage(user.getEmail(), subject, htmlContent);
                log.info("Approval confirmation email sent to {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send approval email to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        if ("USER_REGISTRATION".equals(approval.getType()) && "REJECTED".equalsIgnoreCase(status)) {
            UserProfile rejectedUser = userRepository.findById(approval.getReferenceId()).orElse(null);
            if (rejectedUser != null) {
                rejectedUser.setAccountStatus(AccountStatus.REJECTED);
                userRepository.findById(approverId).ifPresent(rejectedUser::setApprovedBy);
                rejectedUser.setApprovedAt(LocalDateTime.now());
                userRepository.save(rejectedUser);

                try {
                    String subject = "ROCS - Registration Request Update";
                    String htmlContent = com.silverline.erp.infrastructure.email.TemplateEngine.loadAndResolve(
                            "registration_rejected",
                            Map.of("fullName", rejectedUser.getFullName())
                    );

                    emailService.sendHtmlMessage(rejectedUser.getEmail(), subject, htmlContent);
                    log.info("Rejection notification email sent to {}", rejectedUser.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send rejection email to {}: {}", rejectedUser.getEmail(), e.getMessage());
                }
            }
        }

        if (approval.getType() != null && approval.getType().startsWith("CASH_FLOW_")) {
            CashFlow cashFlow = cashReconciliationService.findCashFlowById(approval.getReferenceId());
            if (cashFlow == null) {
                throw new RuntimeException("Cash Flow not found: " + approval.getReferenceId());
            }

            cashFlow.setStatus(status.toUpperCase());
            cashReconciliationService.saveCashFlow(cashFlow);
        }

        approvalRepository.save(approval);

        UserProfile approver = null;
        if (approval.getApprovedBy() != null) {
            approver = userRepository.findById(approval.getApprovedBy()).orElse(null);
        }

        UserProfile requester = null;
        if (approval.getRequestedBy() != null) {
            requester = userRepository.findById(approval.getRequestedBy()).orElse(null);
        }

        String branchName = "-";
        if (approval.getBranchId() != null) {
            branchName = branchRepository.findById(approval.getBranchId())
                    .map(Branch::getName)
                    .orElse("-");
        }

        return mapToApprovalDTO(approval, requester, approver, branchName);
    }

    private ApprovalDTO mapToApprovalDTO(Approval approval, UserProfile requester, UserProfile approver, String branchName) {
        String name = requester != null ? requester.getFullName() : "Unknown";
        String username = requester != null ? requester.getUsername() : "-";
        String email = requester != null ? requester.getEmail() : "-";
        String phone = requester != null ? requester.getPhone() : "-";
        String approvedByName = approver != null ? approver.getFullName() : "-";

        BigDecimal amount = null;
        String reason = approval.getRequestNotes();
        String type = approval.getType();
        String referenceNo = approval.getReferenceNo();

        if (approval.getType() != null && approval.getType().startsWith("CASH_FLOW_") && approval.getReferenceId() != null) {
            CashFlow cashFlow = cashReconciliationService.findCashFlowById(approval.getReferenceId());
            if (cashFlow != null) {
                amount = cashFlow.getAmount();
                reason = cashFlow.getReason();
                type = cashFlow.getType();
                if (cashFlow.getReferenceNo() != null && !cashFlow.getReferenceNo().isEmpty()) {
                    referenceNo = cashFlow.getReferenceNo();
                }
            }
        }

        return ApprovalDTO.builder()
                .id(approval.getApprovalId())
                .category(approval.getType())
                .reference(referenceNo != null ? referenceNo : "REF-" + approval.getReferenceId())
                .requestedBy(name)
                .branchName(branchName)
                .username(username)
                .email(email)
                .phone(phone)
                .approvedBy(approvedByName)
                .time(formatDateTime(approval.getCreatedAt()))
                .approvedAt(formatDateTime(approval.getApprovedAt()))
                .status(capitalizeFirst(approval.getStatus()))
                .description(approval.getRequestNotes())
                .amount(amount)
                .reason(reason)
                .type(type)
                .referenceNo(referenceNo)
                .referenceId(approval.getReferenceId())
                .build();
    }

    // ===== BRANCH ACTIVITY LOG =====

    @Override
    public List<ActivityLogDTO> getBranchActivityLog(int limit, Long branchId) {
        List<UserActivityLog> activities;

        if (branchId != null) {
            activities = activityLogRepository.findByBranchId(
                    branchId,
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
        } else {
            activities = activityLogRepository.findAll(
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
        }

        List<Long> userIds = activities.stream()
                .map(a -> a.getUserId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> userNames = userIds.isEmpty() ? Map.of() :
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(u -> u.getUserId(), u -> u.getFullName(), (a, b) -> a));

        return activities.stream()
                .map(activity -> {
                    String userName = userNames.getOrDefault(activity.getUserId(), "System");
                    return ActivityLogDTO.builder()
                            .activityId(activity.getActivityId())
                            .time(formatDateTime(activity.getCreatedAt()))
                            .user(userName)
                            .action(activity.getActivityType())
                            .details(activity.getDescription())
                            .severity(determineSeverity(activity.getActivityType()))
                            .actionType(activity.getActivityType())
                            .username(userName)
                            .description(activity.getDescription())
                            .createdAt(activity.getCreatedAt() != null ? activity.getCreatedAt().toString() : null)
                            .status("SUCCESS")
                            .branchId(activity.getBranchId())
                            .userId(activity.getUserId())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public org.springframework.data.domain.Page<ActivityLogDTO> getBranchActivityLog(org.springframework.data.domain.Pageable pageable, Long branchId) {
        org.springframework.data.domain.Pageable capped = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100),
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt"));

        org.springframework.data.domain.Page<UserActivityLog> activitiesPage;
        if (branchId != null) {
            activitiesPage = activityLogRepository.findByBranchId(branchId, capped);
        } else {
            activitiesPage = activityLogRepository.findAll(capped);
        }

        List<Long> userIds = activitiesPage.getContent().stream()
                .map(a -> a.getUserId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> userNames = userIds.isEmpty() ? Map.of() :
                userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(u -> u.getUserId(), u -> u.getFullName(), (a, b) -> a));

        return activitiesPage.map(activity -> {
            String userName = userNames.getOrDefault(activity.getUserId(), "System");
            return ActivityLogDTO.builder()
                    .activityId(activity.getActivityId())
                    .time(formatDateTime(activity.getCreatedAt()))
                    .user(userName)
                    .action(activity.getActivityType())
                    .details(activity.getDescription())
                    .severity(determineSeverity(activity.getActivityType()))
                    .actionType(activity.getActivityType())
                    .username(userName)
                    .description(activity.getDescription())
                    .createdAt(activity.getCreatedAt() != null ? activity.getCreatedAt().toString() : null)
                    .status("SUCCESS")
                    .branchId(activity.getBranchId())
                    .userId(activity.getUserId())
                    .build();
        });
    }

    // ===== HELPER METHODS =====

    private String formatLastLogin(LocalDateTime lastLogin) {
        if (lastLogin == null) return "Never";
        return lastLogin.format(DATE_TIME_FORMATTER);
    }

    private String determineUserStatus(UserProfile user) {
        if (user.getAccountStatus() == AccountStatus.REJECTED) {
            return "Rejected";
        }
        LocalDateTime lastLogin = user.getLastLogin();
        if (lastLogin == null) return "Inactive";
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        return lastLogin.isAfter(threshold) ? "Active" : "Offline";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String determineSeverity(String activityType) {
        if (activityType == null) return "Info";
        String upper = activityType.toUpperCase();
        if (upper.contains("DELETE") || upper.contains("ERROR") || upper.contains("FAIL")) {
            return "Critical";
        }
        if (upper.contains("UPDATE") || upper.contains("MODIFY") || upper.contains("CHANGE")) {
            return "Warning";
        }
        return "Info";
    }
}
