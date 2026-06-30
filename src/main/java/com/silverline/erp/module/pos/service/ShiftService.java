package com.silverline.erp.module.pos.service;

import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.module.pos.dto.shift.ShiftResponse;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.domain.pos.CashFlow;

import com.silverline.erp.domain.pos.CashShift;
import com.silverline.erp.module.pos.dto.CashFlowRequest;
import com.silverline.erp.module.pos.dto.ShiftStartRequest;
import com.silverline.erp.module.pos.dto.shift.CloseShiftRequest;
import com.silverline.erp.module.pos.repository.ShiftRepository;
import com.silverline.erp.module.pos.repository.CashFlowRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.module.pos.repository.SalesReturnRepository;
import com.silverline.erp.module.pos.repository.PaymentRepository;
import com.silverline.erp.common.audit.repository.ApprovalRepository;
import com.silverline.erp.domain.audit.Approval;
import com.silverline.erp.common.audit.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final AuthenticationManager authenticationManager;
    private final UserProfileRepo userProfileRepo;
    private final CashFlowRepository cashFlowRepository;
    private final SaleRepository saleRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final PaymentRepository paymentRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditLogService activityLogService;

    @Autowired
    public ShiftService(ShiftRepository shiftRepository, 
                        AuthenticationManager authenticationManager, 
                        UserProfileRepo userProfileRepo, 
                        CashFlowRepository cashFlowRepository,
                        SaleRepository saleRepository,
                        SalesReturnRepository salesReturnRepository,
                        PaymentRepository paymentRepository,
                        ApprovalRepository approvalRepository,
                        AuditLogService activityLogService) {
        this.shiftRepository = shiftRepository;
        this.authenticationManager = authenticationManager;
        this.userProfileRepo = userProfileRepo;
        this.cashFlowRepository = cashFlowRepository;
        this.saleRepository = saleRepository;
        this.salesReturnRepository = salesReturnRepository;
        this.paymentRepository = paymentRepository;
        this.approvalRepository = approvalRepository;
        this.activityLogService = activityLogService;
    }

    // --- 1. START SHIFT ---
    @Transactional
    public Long startShift(ShiftStartRequest request) {
        Long supervisorId = null;
        LocalDateTime approvedAt = null;

        // Validate Supervisor Credentials if provided
        if (request.getSupervisorUsername() != null &&
                !request.getSupervisorUsername().isEmpty()) {

            String username = request.getSupervisorUsername();
            String password = request.getSupervisorPassword();

            if (password == null || password.isEmpty()) {
                throw new RuntimeException("Supervisor password is required");
            }

            try {
                // First check if user exists
                UserProfile supervisor = userProfileRepo.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Supervisor user not found: " + username));

                // Check if account is active
                if (supervisor.getAccountStatus() != com.silverline.erp.domain.enums.AccountStatus.ACTIVE) {
                    throw new RuntimeException("Supervisor account is not active. Current status: " + supervisor.getAccountStatus());
                }

                // Authenticate using Spring Security
                Authentication auth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, password)
                );

                if (!auth.isAuthenticated()) {
                    throw new RuntimeException("Invalid supervisor credentials");
                }

                // Check role - allow SUPER_ADMIN, ADMIN, MANAGER, or SUPERVISOR
                String role = supervisor.getRole() != null ? supervisor.getRole().name().toUpperCase() : "";
                if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role) && !"SUPERVISOR".equals(role)) {
                    throw new RuntimeException("Access Denied: User " + supervisor.getUsername() + " is a " + role + " and cannot approve shifts.");
                }

                supervisorId = supervisor.getUserId();
                approvedAt = LocalDateTime.now();

            } catch (org.springframework.security.authentication.DisabledException e) {
                throw new RuntimeException("Supervisor account is disabled or not active");
            } catch (org.springframework.security.authentication.BadCredentialsException e) {
                throw new RuntimeException("Invalid supervisor password");
            } catch (org.springframework.security.core.AuthenticationException e) {
                throw new RuntimeException("Authentication Failed: " + e.getMessage());
            } catch (RuntimeException e) {
                throw e; // Re-throw our custom exceptions
            } catch (Exception e) {
                throw new RuntimeException("Approval Failed: " + e.getMessage());
            }
        }

        if (shiftRepository.hasOpenShift(request.getCashierId())) {
            throw new IllegalStateException("Cashier already has an active shift.");
        }

        CashShift shift = new CashShift();
        shift.setShiftNo(generateShiftNo(request.getBranchId()));
        shift.setBranchId(request.getBranchId());
        // NOTE: terminalId REMOVED
        shift.setCashierId(request.getCashierId());
        shift.setOpeningCash(request.getOpeningCash());
        shift.setOpenedAt(LocalDateTime.now());
        // Ensure you are using the String value if ShiftStatus is not imported, or CashShift.ShiftStatus.OPEN
        shift.setStatus(CashShift.ShiftStatus.valueOf("OPEN"));
        shift.setApprovedBy(supervisorId);
        shift.setApprovedAt(approvedAt);

        // Initialize totals
        shift.setTotalSales(BigDecimal.ZERO);
        shift.setTotalReturns(BigDecimal.ZERO);
        shift.setClosingCash(BigDecimal.ZERO);
        shift.setExpectedCash(request.getOpeningCash());
        shift.setCashDifference(BigDecimal.ZERO);

        Long savedShiftId = shiftRepository.save(shift);
        shift.setShiftId(savedShiftId);
     
        // Fetch username for logging
        String cashierUsername = userProfileRepo.findById(request.getCashierId())
                .map(UserProfile::getUsername)
                .orElse("Cashier #" + request.getCashierId());

        // Log Activity
        activityLogService.logActivity(
            request.getBranchId(),
            null, // terminalId removed
            request.getCashierId(),
            cashierUsername,
            "CASHIER",
            "SHIFT_OPEN",
            "SHIFT",
            savedShiftId,
            "Shift #" + shift.getShiftNo() + " opened with opening cash " + request.getOpeningCash(),
            "{\"openingCash\":\"" + request.getOpeningCash() + "\"}"
        );

        return savedShiftId;
    }

    // --- 2. GET ACTIVE SHIFT ID ---
    public Long getActiveShiftId(Long cashierId) {
        return shiftRepository.findOpenShiftByCashierId(cashierId)
                .map(CashShift::getShiftId)
                .orElseThrow(() -> new IllegalStateException("No active shift found for this cashier. Please open a shift."));
    }

    // --- 3. GET SHIFT TOTALS ---
    // --- 3. GET SHIFT TOTALS ---
    public Map<String, Object> getShiftTotals(Long shiftId) {
        CashShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        // 1. Sales
        BigDecimal sales = saleRepository.sumNetTotalByShiftId(shiftId);
        if (sales == null) sales = BigDecimal.ZERO;

        // 2. Returns
        BigDecimal returns = salesReturnRepository.sumTotalAmountByShiftId(shiftId);
        if (returns == null) returns = BigDecimal.ZERO;

        // 3. Cash Flows
        BigDecimal paidIn = cashFlowRepository.getTotalByTypeAndStatus(shiftId, "PAID_IN", "APPROVED");
        if (paidIn == null) paidIn = BigDecimal.ZERO;
        
        BigDecimal paidOut = cashFlowRepository.getTotalByTypeAndStatus(shiftId, "PAID_OUT", "APPROVED");
        if (paidOut == null) paidOut = BigDecimal.ZERO;

        // Pending Cash Flows
        BigDecimal pendingPaidIn = cashFlowRepository.getTotalByTypeAndStatus(shiftId, "PAID_IN", "PENDING");
        if (pendingPaidIn == null) pendingPaidIn = BigDecimal.ZERO;

        BigDecimal pendingPaidOut = cashFlowRepository.getTotalByTypeAndStatus(shiftId, "PAID_OUT", "PENDING");
        if (pendingPaidOut == null) pendingPaidOut = BigDecimal.ZERO;

        long pendingRequests = cashFlowRepository.countByShiftIdAndStatus(shiftId, "PENDING");

        // Breakdowns
        BigDecimal cashSales = paymentRepository.sumTotalByShiftIdAndType(shiftId, "CASH");
        if (cashSales == null) cashSales = BigDecimal.ZERO;

        BigDecimal cardSales = paymentRepository.sumTotalByShiftIdAndType(shiftId, "CARD");
        if (cardSales == null) cardSales = BigDecimal.ZERO;

        BigDecimal otherSales = paymentRepository.sumOtherPaymentsByShiftId(shiftId);
        if (otherSales == null) otherSales = BigDecimal.ZERO;

        Map<String, Object> totals = new HashMap<>();
        totals.put("shiftId", shift.getShiftId());
        totals.put("openingCash", shift.getOpeningCash());
        totals.put("totalSales", sales);
        totals.put("cashSales", cashSales);
        totals.put("cardTotal", cardSales);
        totals.put("otherPayments", otherSales);
        totals.put("totalReturns", returns);
        totals.put("paidIn", paidIn);
        totals.put("paidOut", paidOut);
        totals.put("pendingPaidIn", pendingPaidIn);
        totals.put("pendingPaidOut", pendingPaidOut);
        totals.put("pendingRequests", pendingRequests);

        // Expected Cash = Opening + Cash Sales + Paid In - Paid Out - Returns (Assuming returns are cash for safety, or refine later)
        // If we strictly want Cash in Drawer, we should use cashSales.
        // Total Sales includes Card. We shouldn't add Card sales to expected CASH.
        BigDecimal expected = shift.getOpeningCash()
                .add(cashSales)
                .subtract(returns)
                .add(paidIn)
                .subtract(paidOut);
        
        totals.put("expectedCash", expected);

        // Transaction count
        int transactionCount = saleRepository.countByShiftId(shiftId);
        totals.put("transactionCount", transactionCount);

        return totals;
    }



    // --- 4. CLOSE SHIFT ---
    @Transactional
    public void closeShift(Long cashierId, CloseShiftRequest request) {
        CashShift shift = shiftRepository.findOpenShiftByCashierId(cashierId)
                .orElseThrow(() -> new IllegalStateException("No open shift found for this cashier"));
        
        // Check for pending approvals
        long pending = cashFlowRepository.countByShiftIdAndStatus(shift.getShiftId(), "PENDING");
        if (pending > 0) {
            throw new IllegalStateException("Cannot close shift with " + pending + " pending cash flow requests. waiting for approval.");
        }

        performCloseShift(shift, request);
    }

    @Transactional
    public void closeShiftById(Long shiftId, CloseShiftRequest request) {
        CashShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found: " + shiftId));

        if (shift.getStatus() != CashShift.ShiftStatus.OPEN) {
            throw new IllegalStateException("Shift is not open/active. Status: " + shift.getStatus());
        }

        performCloseShift(shift, request);
    }

    private void performCloseShift(CashShift shift, CloseShiftRequest request) {
        shift = shiftRepository.findByIdWithStats(shift.getShiftId()).orElse(shift);

        BigDecimal totalSales = shift.getTotalSales() != null ? shift.getTotalSales() : BigDecimal.ZERO;
        BigDecimal totalReturns = shift.getTotalReturns() != null ? shift.getTotalReturns() : BigDecimal.ZERO;
        BigDecimal expected = shift.getOpeningCash().add(totalSales).subtract(totalReturns);

        shift.setClosingCash(request.getClosingCash());
        shift.setExpectedCash(expected);
        shift.setCashDifference(request.getClosingCash().subtract(expected));
        shift.setClosedAt(LocalDateTime.now());
        shift.setStatus(CashShift.ShiftStatus.valueOf("CLOSED"));
        shift.setNotes(request.getNotes());

        shiftRepository.update(shift);

        // Fetch username for logging
        String cashierUsername = userProfileRepo.findById(shift.getCashierId())
                .map(UserProfile::getUsername)
                .orElse("Cashier #" + shift.getCashierId());

        // Log Activity â€” metadata must be valid JSON for MySQL JSON column
        String closeMetadata = "{\"expected\":\"" + expected + "\",\"difference\":\"" + shift.getCashDifference() + "\",\"closingCash\":\"" + request.getClosingCash() + "\"}";
        activityLogService.logActivity(
            shift.getBranchId(),
            null, // terminalId removed
            shift.getCashierId(),
            cashierUsername,
            "CASHIER",
            "SHIFT_CLOSE",
            "SHIFT",
            shift.getShiftId(),
            "Shift #" + shift.getShiftNo() + " closed. Closing Cash: " + request.getClosingCash(),
            closeMetadata
        );
    }

    // --- 5. RECORD CASH FLOW ---
    @Transactional
    public CashFlow recordCashFlow(Long cashierId, CashFlowRequest request) {
        CashShift shift;

        if (request.getShiftId() != null) {
            shift = shiftRepository.findById(request.getShiftId())
                    .orElseThrow(() -> new IllegalStateException("Shift not found: " + request.getShiftId()));
            
            // Optionally check if it's OPEN, though not strictly required if we just want to record it
            if (shift.getStatus() != CashShift.ShiftStatus.OPEN) {
                 throw new IllegalStateException("Cannot record cash flow for closed shift: " + request.getShiftId());
            }
        } else {
            shift = shiftRepository.findOpenShiftByCashierId(cashierId)
                    .orElseThrow(() -> new IllegalStateException("No open shift found for this cashier"));
        }
        
        CashFlow flow = new CashFlow();
        flow.setShiftId(shift.getShiftId());
        flow.setCreatedBy(cashierId);
        flow.setCreatedAt(LocalDateTime.now());
        flow.setType(request.getType());
        flow.setAmount(request.getAmount());
        flow.setReason(request.getReason());
        flow.setReferenceNo(request.getReferenceNo());
        flow.setStatus("PENDING"); // Pending Manager Approval
        
        CashFlow savedFlow = cashFlowRepository.save(flow);

        // Auto-approve Logic (if small amount, etc) - for now it's PENDING

        // Create Approval Request
        if ("PENDING".equals(savedFlow.getStatus())) {
             Approval approval = new Approval();
             approval.setReferenceId(savedFlow.getFlowId());
             approval.setType("CASH_FLOW_" + flow.getType()); // CASH_FLOW_PAID_IN / OUT
             approval.setStatus("PENDING");
             approval.setRequestedBy(cashierId);
             approval.setCreatedAt(LocalDateTime.now());
             approval.setBranchId(shift.getBranchId());
             
             // Reference info
             approval.setReferenceNo("CF-" + savedFlow.getFlowId());
             approval.setRequestNotes(request.getType() + ": " + request.getAmount() + " - " + request.getReason());
             
             approvalRepository.save(approval);
        }

        // Fetch username for logging
        String cashierUsername = userProfileRepo.findById(cashierId)
                .map(UserProfile::getUsername)
                .orElse("Cashier #" + cashierId);

        // Log Activity
        activityLogService.logActivity(
            shift.getBranchId(),
            null, // terminalId removed
            cashierId,
            cashierUsername,
            "CASHIER",
            "CASH_FLOW_REQUEST",
            "CASH_FLOW",
            savedFlow.getFlowId(),
            "Requested " + request.getType() + " of " + request.getAmount(),
            "{\"reason\":\"" + (request.getReason() != null ? request.getReason().replace("\"", "'") : "") + "\",\"amount\":\"" + request.getAmount() + "\"}"
        );

        return savedFlow;
    }

    // --- 6. GET ACTIVE SHIFT ---
    public com.silverline.erp.module.pos.dto.shift.ShiftResponse getActiveShift(Long branchId, Long cashierId) {
        CashShift shift = null;

        if (cashierId != null) {
            shift = shiftRepository.findOpenShiftByCashierId(cashierId)
                    .map(s -> shiftRepository.findByIdWithStats(s.getShiftId()).orElse(s))
                    .orElse(null);
        }

        // Fallback: Check branch if cashierId not provided
        if (shift == null && branchId != null) {
             shift = shiftRepository.findOpenShiftByBranchId(branchId)
                    .map(s -> shiftRepository.findByIdWithStats(s.getShiftId()).orElse(s))
                    .orElse(null);
        }

        if (shift == null) return null;

        String cashierName = userProfileRepo.findById(shift.getCashierId())
                .map(UserProfile::getFullName)
                .orElse(userProfileRepo.findById(shift.getCashierId())
                        .map(UserProfile::getUsername)
                        .orElse("Unknown Cashier"));

        return new com.silverline.erp.module.pos.dto.shift.ShiftResponse.Builder()
                .shiftId(shift.getShiftId())
                .cashierId(shift.getCashierId())
                .cashierName(cashierName)
                .branchId(shift.getBranchId())
                .openingCash(shift.getOpeningCash())
                .totalSales(shift.getTotalSales())
                .totalReturns(shift.getTotalReturns())
                .openedAt(shift.getOpenedAt())
                .status(shift.getStatus().name())
                .build();
    }

    // --- 7. GET CASHIERS ---
    public List<UserProfile> getCashiersByBranch(Long branchId) {
        // NOTE: findByBranch_BranchIdAndRole removed â€” fetch all cashiers
        return userProfileRepo.findByRole(com.silverline.erp.domain.enums.Role.CASHIER);
    }

    public List<CashFlow> getShiftCashFlows(Long shiftId) {
        return cashFlowRepository.findByShiftId(shiftId);
    }

    private String generateShiftNo(Long branchId) {
        return "SH-" + branchId + "-" + System.currentTimeMillis();
    }
}

