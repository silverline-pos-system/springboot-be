package com.silverline.erp.module.manager.service;

import com.silverline.erp.common.email.EmailService;
import com.silverline.erp.common.security.SecurityUtils;
import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.finance.Account;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.user.Permission;
import com.silverline.erp.module.analytics.dto.BranchAlertDTO;
import com.silverline.erp.module.analytics.dto.DashboardStatsDTO;
import com.silverline.erp.module.analytics.dto.ExpiryAlertDTO;
import com.silverline.erp.module.analytics.dto.HourlySalesDTO;
import com.silverline.erp.module.analytics.dto.PaymentBreakdownDTO;
import com.silverline.erp.module.analytics.dto.RecentTransactionDTO;
import com.silverline.erp.module.analytics.dto.SalesAnalyticsDTO;
import com.silverline.erp.module.analytics.dto.SalesDataDTO;
import com.silverline.erp.module.analytics.dto.SalesReportDTO;
import com.silverline.erp.module.analytics.dto.StaffSummaryDTO;
import com.silverline.erp.module.analytics.dto.StockAlertDTO;
import com.silverline.erp.module.analytics.dto.TerminalSalesDTO;
import com.silverline.erp.module.analytics.dto.TopSellingProductDTO;
import com.silverline.erp.module.manager.dto.ActivityLogDTO;
import com.silverline.erp.module.manager.dto.ApprovalDTO;
import com.silverline.erp.module.manager.dto.PendingDispatchDTO;
import com.silverline.erp.domain.audit.Approval;
import com.silverline.erp.domain.audit.UserActivityLog;
import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.domain.inventory.Dispatch;
import com.silverline.erp.domain.inventory.Product;
import com.silverline.erp.domain.inventory.Supplier;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.module.inventory.repository.BatchRepository;
import com.silverline.erp.module.procurement.repository.DispatchRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.SupplierRepository;
import com.silverline.erp.module.manager.dto.*;
import com.silverline.erp.module.finance.dto.*;
import com.silverline.erp.module.analytics.dto.*;
import com.silverline.erp.module.manager.repository.ManagerSaleItemRepository;
import com.silverline.erp.module.manager.repository.ManagerSaleRepository;
import com.silverline.erp.module.manager.repository.ManagerUserRepository;
import com.silverline.erp.common.audit.repository.ApprovalRepository;
import com.silverline.erp.common.audit.repository.UserActivityLogRepository;
import com.silverline.erp.module.pos.repository.CashFlowRepository;
import com.silverline.erp.module.pos.repository.PaymentRepository;
import com.silverline.erp.module.pos.repository.SalesReturnRepository;
import com.silverline.erp.module.pos.repository.SaleItemRepository;
import com.silverline.erp.domain.pos.CashFlow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerService {

    private final ManagerSaleRepository saleRepository;
    private final ManagerSaleItemRepository saleItemRepository;
    private final ManagerUserRepository userRepository;
    private final DispatchRepository dispatchRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final SupplierRepository supplierRepository;
    private final ApprovalRepository approvalRepository;
    private final UserActivityLogRepository activityLogRepository;
    private final CashFlowRepository cashFlowRepository;
    private final PaymentRepository paymentRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final SaleItemRepository posSaleItemRepository;
    // NOTE: UserBranchRepository REMOVED â€” users are NOT tied to branches
    private final com.silverline.erp.common.email.EmailService emailService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ===== DASHBOARD STATS =====


    public List<DashboardStatsDTO> getDashboardStats(Long branchId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);

        // Branch-specific sales
        BigDecimal todaySales;
        Long todayTransactions;
        BigDecimal yesterdaySales;
        List<Dispatch> pendingDispatches;

        if (branchId != null) {
            todaySales = saleRepository.sumNetTotalByBranchAndDateRange(branchId, todayStart, todayEnd);
            todayTransactions = saleRepository.countByBranchAndDateRange(branchId, todayStart, todayEnd);
            yesterdaySales = saleRepository.sumNetTotalByBranchAndDateRange(branchId, yesterdayStart, yesterdayEnd);
            pendingDispatches = dispatchRepository.findByBranchIdAndStatus(branchId, "PENDING");
        } else {
            todaySales = saleRepository.sumNetTotalByDateRange(todayStart, todayEnd);
            todayTransactions = saleRepository.countByDateRange(todayStart, todayEnd);
            yesterdaySales = saleRepository.sumNetTotalByDateRange(yesterdayStart, yesterdayEnd);
            pendingDispatches = dispatchRepository.findByStatus("PENDING");
        }

        todaySales = todaySales != null ? todaySales : BigDecimal.ZERO;
        yesterdaySales = yesterdaySales != null ? yesterdaySales : BigDecimal.ZERO;

        // Low stock count
        List<StockAlertDTO> lowStockAlerts = getStockAlerts(branchId);

        List<DashboardStatsDTO> stats = new ArrayList<>();

        // Today's Sales stat
        stats.add(DashboardStatsDTO.builder()
                .title("Today's Sales")
                .value(formatCurrency(todaySales))
                .icon("currency")
                .tone(todaySales.compareTo(yesterdaySales) >= 0 ? "success" : "warning")
                .build());

        // Transactions stat
        stats.add(DashboardStatsDTO.builder()
                .title("Transactions")
                .value(String.valueOf(todayTransactions))
                .icon("receipt")
                .tone("info")
                .build());

        // pending dispatches stat
        stats.add(DashboardStatsDTO.builder()
                .title("pending dispatches")
                .value(String.valueOf(pendingDispatches.size()))
                .icon("truck")
                .tone(pendingDispatches.isEmpty() ? "success" : "warning")
                .build());

        // Low Stock stat
        stats.add(DashboardStatsDTO.builder()
                .title("Low Stock Items")
                .value(String.valueOf(lowStockAlerts.size()))
                .icon("package")
                .tone(lowStockAlerts.isEmpty() ? "success" : "danger")
                .build());

        return stats;
    }

    public List<SalesDataDTO> getSalesData(String period, Long branchId) {
        int days = switch (period.toLowerCase()) {
            case "daily" -> 1;
            case "monthly" -> 30;
            default -> 7;
        };

        List<SalesDataDTO> salesData = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("EEE");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            BigDecimal dailySales;
            Long transactions;
            if (branchId != null) {
                dailySales = saleRepository.sumNetTotalByBranchAndDateRange(branchId, dayStart, dayEnd);
                transactions = saleRepository.countByBranchAndDateRange(branchId, dayStart, dayEnd);
            } else {
                dailySales = saleRepository.sumNetTotalByDateRange(dayStart, dayEnd);
                transactions = saleRepository.countByDateRange(dayStart, dayEnd);
            }

            salesData.add(SalesDataDTO.builder()
                    .label(date.format(labelFormatter))
                    .value(dailySales != null ? dailySales : BigDecimal.ZERO)
                    .transactions(transactions != null ? transactions.intValue() : 0)
                    .build());
        }

        return salesData;
    }

    // ===== TOP SELLING PRODUCTS =====

    public List<TopSellingProductDTO> getTopSellingProducts(int limit, Long branchId) {
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime weekEnd = LocalDateTime.now();

        List<Object[]> results;
        if (branchId != null) {
            results = saleItemRepository.findTopSellingProductsByBranch(branchId, weekStart, weekEnd, limit);
        } else {
            results = saleItemRepository.findTopSellingProducts(weekStart, weekEnd, limit);
        }

        return results.stream()
                .map(row -> TopSellingProductDTO.builder()
                        .productId(((Number) row[0]).longValue())
                        .name((String) row[1])
                        .sku((String) row[2])
                        .units(((Number) row[3]).intValue())
                        .revenue(formatCurrency((BigDecimal) row[4]))
                        .build())
                .collect(Collectors.toList());
    }

    // ===== pending dispatches =====

    public List<PendingDispatchDTO> getPendingDispatches(Long branchId) {
        List<Dispatch> pendingDispatches;
        if (branchId != null) {
            pendingDispatches = dispatchRepository.findByBranchIdAndStatus(branchId, "PENDING");
        } else {
            pendingDispatches = dispatchRepository.findByStatus("PENDING");
        }

        return pendingDispatches.stream()
                .map(dispatch -> {
                    String supplierName = getSupplierName(dispatch.getSupplierId());
                    String eta = calculateEta(dispatch.getDispatchDate());

                    return PendingDispatchDTO.builder()
                            .id(dispatch.getDispatchNo())
                            .supplier(supplierName)
                            .items(0) // Would need dispatch items count
                            .eta(eta)
                            .requestedBy(dispatch.getCreatedBy() != null ? 
                                userRepository.findById(dispatch.getCreatedBy())
                                    .map(u -> u.getFullName())
                                    .orElse("ID: " + dispatch.getCreatedBy()) : "System")
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ===== STAFF SUMMARY =====

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
                        .status(determineUserStatus(user.getLastLogin()))
                        .approvedBy(user.getApprovedBy() != null ? user.getApprovedBy().getFullName() : "System")
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .employeeId(user.getEmployeeId())
                        .build())
                .collect(Collectors.toList());
    }

    // ===== STOCK ALERTS =====

    public List<StockAlertDTO> getStockAlerts(Long branchId) {
        List<Product> products = productRepository.findByIsActiveTrue();
        List<StockAlertDTO> alerts = new ArrayList<>();

        for (Product product : products) {
            // Sum up batch quantities for this product in specific branch if provided
            List<Batch> batches;
            if (branchId != null) {
                batches = batchRepository.findByProductIdAndBranchId(product.getProductId(), branchId);
            } else {
                batches = batchRepository.findByProductId(product.getProductId());
            }

            BigDecimal totalQty = batches.stream()
                    .map(Batch::getQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal reorderLevel = product.getReorderLevel() != null ? product.getReorderLevel() : BigDecimal.ZERO;

            if (totalQty.compareTo(reorderLevel) <= 0) {
                String level = totalQty.compareTo(BigDecimal.ZERO) == 0 ? "Critical" : "Low";

                alerts.add(StockAlertDTO.builder()
                        .productId(product.getProductId())
                        .item(product.getName())
                        .qty(totalQty)
                        .level(level)
                        .build());
            }
        }

        return alerts;
    }

    // ===== EXPIRY ALERTS =====

    public List<ExpiryAlertDTO> getExpiryAlerts(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);

        List<Batch> expiringBatches = batchRepository.findExpiringSoonBatchesByBranch(today, thirtyDaysFromNow, branchId);

        Map<Long, String> productNames = productRepository.findByIsActiveTrue().stream()
                .collect(Collectors.toMap(Product::getProductId, Product::getName));

        return expiringBatches.stream()
                .map(batch -> {
                    long daysUntilExpiry = ChronoUnit.DAYS.between(today, batch.getExpiryDate());
                    String severity = daysUntilExpiry <= 7 ? "Critical" : daysUntilExpiry <= 14 ? "Warning" : "Info";

                    return ExpiryAlertDTO.builder()
                            .batchId(batch.getBatchId())
                            .productId(batch.getProductId())
                            .item(productNames.getOrDefault(batch.getProductId(), "Unknown"))
                            .expiresOn(batch.getExpiryDate().toString())
                            .qty(batch.getQty())
                            .severity(severity)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ===== BRANCH ALERTS =====

    public List<BranchAlertDTO> getBranchAlerts(Long branchId) {
        List<BranchAlertDTO> alerts = new ArrayList<>();

        // Low stock alerts
        List<StockAlertDTO> stockAlerts = getStockAlerts(branchId); // This will now call getStockAlerts(null)
        for (StockAlertDTO stockAlert : stockAlerts) {
            alerts.add(BranchAlertDTO.builder()
                    .alertId((long) alerts.size() + 1)
                    .message("Low stock: " + stockAlert.getItem() + " (" + stockAlert.getQty() + " left)")
                    .time("Now")
                    .type(stockAlert.getLevel())
                    .build());
        }

        // Expiry alerts
        List<ExpiryAlertDTO> expiryAlerts = getExpiryAlerts(branchId);
        for (ExpiryAlertDTO expiryAlert : expiryAlerts) {
            if ("Critical".equals(expiryAlert.getSeverity())) {
                alerts.add(BranchAlertDTO.builder()
                        .alertId((long) alerts.size() + 1)
                        .message("Expiring soon: " + expiryAlert.getItem() + " on " + expiryAlert.getExpiresOn())
                        .time("Now")
                        .type("Warning")
                        .build());
            }
        }

        // pending dispatch alerts
        List<PendingDispatchDTO> pendingDispatches = getPendingDispatches(branchId);
        if (!pendingDispatches.isEmpty()) {
            alerts.add(BranchAlertDTO.builder()
                    .alertId((long) alerts.size() + 1)
                    .message(pendingDispatches.size() + " pending dispatch(s) awaiting approval")
                    .time("Now")
                    .type("Info")
                    .build());
        }

        return alerts;
    }

    // Security logic replaced by SecurityUtils.getCurrentUserId()

    public List<ApprovalDTO> getMyApprovals(Long branchId) {
        Long userId = com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();
        if (userId == null) return new ArrayList<>();

        List<Approval> approvals;
        if (branchId != null) {
            approvals = approvalRepository.findByBranchIdAndStatus(branchId, "PENDING"); 
        } else {
            approvals = approvalRepository.findByStatus("PENDING");
        }
        
        // Efficiently fetch involved users
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        approvals.forEach(a -> {
            if (a.getRequestedBy() != null) userIds.add(a.getRequestedBy());
            if (a.getApprovedBy() != null) userIds.add(a.getApprovedBy());
        });
        
        Map<Long, UserProfile> userMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
             userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, u -> u));
        }

        final Map<Long, UserProfile> finalUserMap = userMap;
        return approvals.stream()
                .map(approval -> mapToApprovalDTO(
                        approval, 
                        finalUserMap.get(approval.getRequestedBy()),
                        finalUserMap.get(approval.getApprovedBy())
                ))
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .collect(Collectors.toList());
    }

    // ===== APPROVALS =====

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
            // All managers see ALL approvals â€” no branch restriction
            if (status != null && !status.isEmpty()) {
                approvals = approvalRepository.findByStatus(status.toUpperCase());
            } else {
                approvals = approvalRepository.findAll();
            }
        }

        // Efficiently fetch users involved in these approvals (requesters and approvers)
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        approvals.forEach(a -> {
            if (a.getRequestedBy() != null) userIds.add(a.getRequestedBy());
            if (a.getApprovedBy() != null) userIds.add(a.getApprovedBy());
        });
        
        Map<Long, UserProfile> userMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
             userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, u -> u));
        }

        final Map<Long, UserProfile> finalUserMap = userMap;
        return approvals.stream()
                .map(approval -> mapToApprovalDTO(
                        approval, 
                        finalUserMap.get(approval.getRequestedBy()),
                        finalUserMap.get(approval.getApprovedBy())
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public ApprovalDTO updateApprovalStatus(Long approvalId, String status, String notes, String role, Long approverId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("Approval not found: " + approvalId));

        Long effectiveApproverId = approverId != null ? approverId : com.silverline.erp.common.security.SecurityUtils.getCurrentUserId();

        approval.setStatus(status.toUpperCase());
        approval.setApprovalNotes(notes);
        approval.setApprovedAt(LocalDateTime.now());
        if (effectiveApproverId != null) {
            approval.setApprovedBy(effectiveApproverId);
        }

        // Handle User Registration Approval logic
        UserProfile user = null;
        if ("USER_REGISTRATION".equals(approval.getType()) && "APPROVED".equalsIgnoreCase(status)) {
            user = userRepository.findById(approval.getReferenceId())
                    .orElseThrow(() -> new RuntimeException("Ref User not found: " + approval.getReferenceId()));

            user.setAccountStatus(AccountStatus.ACTIVE);
            if (effectiveApproverId != null) {
                userRepository.findById(effectiveApproverId).ifPresent(user::setApprovedBy);
                user.setApprovedAt(LocalDateTime.now());
            }
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

            // Send approval confirmation email (without password - user already set it during registration)
            try {
                String assignedRole = user.getRole() != null ? user.getRole().name() : "Staff";
                // NOTE: Branch name removed from email â€” users not tied to branches
                String subject = "Welcome to ROCS - Your Account Has Been Approved!";
                String body = "Dear " + user.getFullName() + ",\n\n" +
                        "Great news! Your registration request has been approved.\n\n" +
                        "Account Details:\n" +
                        "â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”\n" +
                        "Username: " + user.getUsername() + "\n" +
                        "Role: " + assignedRole + "\n" +
                        "â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”\n\n" +
                        "You can now log in using the password you set during registration.\n\n" +
                        "If you have forgotten your password, please use the 'Forgot Password' option on the login page.\n\n" +
                        "Best Regards,\n" +
                        "Management,\n" +
                        "Silverline (pvt) ltd.";

                emailService.sendSimpleMessage(user.getEmail(), subject, body);
                log.info("Approval confirmation email sent to {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send approval email to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        // Handle User Registration Rejection - send notification email
        if ("USER_REGISTRATION".equals(approval.getType()) && "REJECTED".equalsIgnoreCase(status)) {
            UserProfile rejectedUser = userRepository.findById(approval.getReferenceId()).orElse(null);
            if (rejectedUser != null) {
                rejectedUser.setAccountStatus(AccountStatus.REJECTED);
                userRepository.save(rejectedUser);

                try {
                    String subject = "ROCS - Registration Request Update";
                    String body = "Dear " + rejectedUser.getFullName() + ",\n\n" +
                            "We regret to inform you that your registration request has not been approved at this time.\n\n" +
                            "If you believe this is an error or would like more information, " +
                            "please contact your branch manager or administrator.\n\n" +
                            "Management,\n" +
                            "Silverline (pvt) ltd.";

                    emailService.sendSimpleMessage(rejectedUser.getEmail(), subject, body);
                    log.info("Rejection notification email sent to {}", rejectedUser.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send rejection email to {}: {}", rejectedUser.getEmail(), e.getMessage());
                }
            }
        }

        // Handle Cash Flow Approval Logic
        if (approval.getType() != null && approval.getType().startsWith("CASH_FLOW_")) {
            CashFlow cashFlow = cashFlowRepository.findById(approval.getReferenceId())
                    .orElseThrow(() -> new RuntimeException("Cash Flow not found: " + approval.getReferenceId()));

            cashFlow.setStatus(status.toUpperCase()); // APPROVED, REJECTED
            cashFlowRepository.save(cashFlow);
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

        return mapToApprovalDTO(approval, requester, approver);
    }

    private ApprovalDTO mapToApprovalDTO(Approval approval, UserProfile requester, UserProfile approver) {
        String name = requester != null ? requester.getFullName() : "Unknown";
        String username = requester != null ? requester.getUsername() : "-";
        String email = requester != null ? requester.getEmail() : "-";
        String approvedByName = approver != null ? approver.getFullName() : "-";

        BigDecimal amount = null;
        String reason = approval.getRequestNotes(); // Default to request notes
        String type = approval.getType();
        String referenceNo = approval.getReferenceNo();

        // Check for specific reference details (e.g. CashFlow)
        if (approval.getType() != null && approval.getType().startsWith("CASH_FLOW_") && approval.getReferenceId() != null) {
            CashFlow cashFlow = cashFlowRepository.findById(approval.getReferenceId()).orElse(null);
            if (cashFlow != null) {
                amount = cashFlow.getAmount();
                reason = cashFlow.getReason(); // Prefer specific reason
                type = cashFlow.getType(); // PAID_IN or PAID_OUT
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
            .username(username)
            .email(email)
            .approvedBy(approvedByName)
            .time(formatDateTime(approval.getCreatedAt()))
            .approvedAt(formatDateTime(approval.getApprovedAt()))
            .status(capitalizeFirst(approval.getStatus()))
            .description(approval.getRequestNotes()) // Keep original notes here
            .amount(amount)
            .reason(reason)
            .type(type)
            .referenceNo(referenceNo)
            .referenceId(approval.getReferenceId())
            .build();
    }

    // ===== BRANCH ACTIVITY LOG =====

    public List<ActivityLogDTO> getBranchActivityLog(int limit, Long branchId) {
        List<UserActivityLog> activities;

        if (branchId != null) {
            // Filter by branch, then sort and limit manually
            activities = activityLogRepository.findByBranchId(branchId).stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            activities = activityLogRepository.findAll(
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
        }

        Map<Long, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getFullName, (a, b) -> a));

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
                            // Additional fields for frontend
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

    // ===== COMPREHENSIVE SALES ANALYTICS =====

    public SalesAnalyticsDTO getSalesAnalytics(String period, Long branchId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().minusDays(1).atTime(LocalTime.MAX);

        // Calculate date range based on period
        int days = switch (period.toLowerCase()) {
            case "daily" -> 1;
            case "monthly" -> 30;
            default -> 7; // weekly
        };
        LocalDateTime periodStart = LocalDate.now().minusDays(days - 1).atStartOfDay();

        // Basic stats
        BigDecimal todaySales;
        BigDecimal yesterdaySales;
        Long todayTransactions;
        Long yesterdayTransactions;
        Long customersServed;

        if (branchId != null) {
            todaySales = saleRepository.sumNetTotalByBranchAndDateRange(branchId, todayStart, todayEnd);
            yesterdaySales = saleRepository.sumNetTotalByBranchAndDateRange(branchId, yesterdayStart, yesterdayEnd);
            todayTransactions = saleRepository.countByBranchAndDateRange(branchId, todayStart, todayEnd);
            yesterdayTransactions = saleRepository.countByBranchAndDateRange(branchId, yesterdayStart, yesterdayEnd);
            customersServed = saleRepository.countDistinctCustomers(todayStart, todayEnd, branchId);
        } else {
            todaySales = saleRepository.sumNetTotalByDateRange(todayStart, todayEnd);
            yesterdaySales = saleRepository.sumNetTotalByDateRange(yesterdayStart, yesterdayEnd);
            todayTransactions = saleRepository.countByDateRange(todayStart, todayEnd);
            yesterdayTransactions = saleRepository.countByDateRange(yesterdayStart, yesterdayEnd);
            customersServed = saleRepository.countDistinctCustomers(todayStart, todayEnd, null);
        }

        todaySales = todaySales != null ? todaySales : BigDecimal.ZERO;
        yesterdaySales = yesterdaySales != null ? yesterdaySales : BigDecimal.ZERO;
        int txnCount = todayTransactions != null ? todayTransactions.intValue() : 0;

        // Calculate average transaction value
        BigDecimal avgTransaction = txnCount > 0 
            ? todaySales.divide(BigDecimal.valueOf(txnCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Calculate growth percentage
        double growth = 0.0;
        if (yesterdaySales.compareTo(BigDecimal.ZERO) > 0) {
            growth = todaySales.subtract(yesterdaySales)
                    .divide(yesterdaySales, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // Payment breakdown
        List<PaymentBreakdownDTO> paymentBreakdown = getPaymentBreakdown(todayStart, todayEnd, branchId);

        // Hourly sales
        List<HourlySalesDTO> hourlySales = getHourlySales(LocalDateTime.now(), branchId);

        // Recent transactions
        List<RecentTransactionDTO> recentTransactions = getRecentTransactions(15, branchId);

        // Top products
        List<TopSellingProductDTO> topProducts = getTopSellingProducts(5, branchId);

        // Daily trend
        List<SalesDataDTO> dailyTrend = getSalesData(period, branchId);

        return SalesAnalyticsDTO.builder()
                .todaySales(todaySales)
                .yesterdaySales(yesterdaySales)
                .weeklyAverage(calculateWeeklyAverage())
                .todayTransactions(txnCount)
                .yesterdayTransactions(yesterdayTransactions != null ? yesterdayTransactions.intValue() : 0)
                .avgTransactionValue(avgTransaction)
                .customersServed(customersServed != null ? customersServed.intValue() : 0)
                .growthPercentage(growth)
                .paymentBreakdown(paymentBreakdown)
                .hourlySales(hourlySales)
                .recentTransactions(recentTransactions)
                .topProducts(topProducts)
                .dailyTrend(dailyTrend)
                .build();
    }

    private BigDecimal calculateWeeklyAverage() {
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime weekEnd = LocalDateTime.now();
        BigDecimal weekTotal = saleRepository.sumNetTotalByDateRange(weekStart, weekEnd);
        return weekTotal != null ? weekTotal.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private List<PaymentBreakdownDTO> getPaymentBreakdown(LocalDateTime startDate, LocalDateTime endDate, Long branchId) {
        List<Object[]> results = paymentRepository.findPaymentBreakdownByDateRange(startDate, endDate, branchId);
        BigDecimal total = results.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return results.stream()
                .map(row -> {
                    String method = (String) row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    int count = ((Number) row[2]).intValue();
                    double percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;

                    return PaymentBreakdownDTO.builder()
                            .method(method != null ? method : "OTHER")
                            .amount(amount)
                            .count(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<HourlySalesDTO> getHourlySales(LocalDateTime targetDate, Long branchId) {
        List<Object[]> results = saleRepository.findHourlySales(targetDate, branchId);
        
        // Create a map for quick lookup
        Map<Integer, Object[]> hourlyMap = results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> row
                ));

        // Build full 24-hour list
        List<HourlySalesDTO> hourlyList = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Object[] data = hourlyMap.get(hour);
            String hourLabel = String.format("%02d:00", hour);
            
            if (data != null) {
                hourlyList.add(HourlySalesDTO.builder()
                        .hour(hourLabel)
                        .sales((BigDecimal) data[1])
                        .transactions(((Number) data[2]).intValue())
                        .build());
            } else {
                hourlyList.add(HourlySalesDTO.builder()
                        .hour(hourLabel)
                        .sales(BigDecimal.ZERO)
                        .transactions(0)
                        .build());
            }
        }
        return hourlyList;
    }

    private List<RecentTransactionDTO> getRecentTransactions(int limit, Long branchId) {
        List<Sale> recentSales;
        if (branchId != null) {
            recentSales = saleRepository.findRecentSalesByBranch(branchId, PageRequest.of(0, limit));
        } else {
            recentSales = saleRepository.findRecentSales(PageRequest.of(0, limit));
        }
        
        Map<Long, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getFullName, (a, b) -> a));

        return recentSales.stream()
                .map(sale -> {
                    // Get item count
                    List<SaleItem> items = posSaleItemRepository.findBySaleId(sale.getSaleId());
                    int itemCount = items != null ? items.size() : 0;
                    
                    // Get payment method
                    List<Payment> payments = paymentRepository.findBySaleId(sale.getSaleId());
                    String paymentMethod = payments != null && !payments.isEmpty() 
                            ? payments.get(0).getPaymentType() 
                            : "CASH";

                    return RecentTransactionDTO.builder()
                            .saleId(sale.getSaleId())
                            .invoiceNo(sale.getInvoiceNo())
                            .cashier(userNames.getOrDefault(sale.getCashierId(), "Unknown"))
                            .itemCount(itemCount)
                            .amount(sale.getNetTotal())
                            .paymentMethod(paymentMethod)
                            .type("SALE")
                            .time(sale.getSaleDate().format(TIME_FORMATTER))
                            .date(sale.getSaleDate().format(DATE_FORMATTER))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ===== SALES REPORTS =====

    public List<SalesReportDTO> getSalesReports(String startDateStr, String endDateStr, Long branchId) {
        LocalDate startDate = startDateStr != null 
                ? LocalDate.parse(startDateStr) 
                : LocalDate.now().minusDays(7);
        LocalDate endDate = endDateStr != null 
                ? LocalDate.parse(endDateStr) 
                : LocalDate.now();

        List<SalesReportDTO> reports = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            // Sales totals
            BigDecimal revenue;
            BigDecimal grossTotal;
            Long invoiceCount;

            if (branchId != null) {
                revenue = saleRepository.sumNetTotalByBranchAndDateRange(branchId, dayStart, dayEnd);
                grossTotal = saleRepository.sumGrossTotalByDateRange(dayStart, dayEnd, branchId);
                invoiceCount = saleRepository.countByBranchAndDateRange(branchId, dayStart, dayEnd);
            } else {
                revenue = saleRepository.sumNetTotalByDateRange(dayStart, dayEnd);
                grossTotal = saleRepository.sumGrossTotalByDateRange(dayStart, dayEnd, null);
                invoiceCount = saleRepository.countByDateRange(dayStart, dayEnd);
            }

            revenue = revenue != null ? revenue : BigDecimal.ZERO;
            grossTotal = grossTotal != null ? grossTotal : BigDecimal.ZERO;
            int invoices = invoiceCount != null ? invoiceCount.intValue() : 0;

            // Payment breakdown
            BigDecimal cashSales = paymentRepository.sumByTypeAndDateRange(dayStart, dayEnd, "CASH", branchId);
            BigDecimal cardSales = paymentRepository.sumByTypeAndDateRange(dayStart, dayEnd, "CARD", branchId);
            BigDecimal qrSales = paymentRepository.sumByTypeAndDateRange(dayStart, dayEnd, "QR", branchId);

            cashSales = cashSales != null ? cashSales : BigDecimal.ZERO;
            cardSales = cardSales != null ? cardSales : BigDecimal.ZERO;
            qrSales = qrSales != null ? qrSales : BigDecimal.ZERO;

            // Returns
            BigDecimal returns = salesReturnRepository.sumTotalAmountByDateRange(dayStart, dayEnd, branchId);
            returns = returns != null ? returns : BigDecimal.ZERO;

            // Calculate cost (estimate as 70% of revenue for now - can be replaced with actual COGS)
            BigDecimal cost = revenue.multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profit = revenue.subtract(cost);

            // Average basket size
            BigDecimal avgBasket = invoices > 0 
                    ? revenue.divide(BigDecimal.valueOf(invoices), 2, RoundingMode.HALF_UP) 
                    : BigDecimal.ZERO;

            // Profit margin
            double profitMargin = revenue.compareTo(BigDecimal.ZERO) > 0
                    ? profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            reports.add(SalesReportDTO.builder()
                    .date(date.format(DATE_FORMATTER))
                    .dayName(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .invoices(invoices)
                    .revenue(revenue)
                    .cost(cost)
                    .profit(profit)
                    .cashSales(cashSales)
                    .cardSales(cardSales)
                    .qrSales(qrSales)
                    .returns(returns)
                    .avgBasket(avgBasket)
                    .profitMargin(profitMargin)
                    .build());
        }

        return reports;
    }

    // ===== HELPER METHODS =====


    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return "LKR " + String.format("%,.0f", amount);
    }

    private String getSupplierName(Long supplierId) {
        if (supplierId == null) return "Unknown";
        return supplierRepository.findById(supplierId)
                .map(Supplier::getName)
                .orElse("Unknown Supplier");
    }

    private String calculateEta(LocalDate dispatchDate) {
        if (dispatchDate == null) return "Unknown";
        LocalDate today = LocalDate.now();
        if (dispatchDate.equals(today)) return "Today";
        if (dispatchDate.equals(today.plusDays(1))) return "Tomorrow";
        if (dispatchDate.isBefore(today)) return "Overdue";
        return dispatchDate.toString();
    }

    private String formatLastLogin(LocalDateTime lastLogin) {
        if (lastLogin == null) return "Never";
        return lastLogin.format(DATE_TIME_FORMATTER);
    }

    private String determineUserStatus(LocalDateTime lastLogin) {
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

    // ===== TERMINAL SALES REPORTS =====
    
    public List<TerminalSalesDTO> getSalesByTerminal(String startDateStr, String endDateStr, Long branchId) {
        // NOTE: Terminals are removed from the system. Returning empty list.
        return java.util.Collections.emptyList();
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


