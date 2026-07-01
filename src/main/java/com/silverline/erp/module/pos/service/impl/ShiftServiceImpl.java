package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.common.event.ShiftClosedEvent;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.pos.CashShift;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.module.pos.dto.ShiftStartRequest;
import com.silverline.erp.module.pos.dto.shift.CloseShiftRequest;
import com.silverline.erp.module.pos.dto.shift.ShiftResponse;
import com.silverline.erp.module.pos.repository.CashFlowRepository;
import com.silverline.erp.module.pos.repository.ShiftRepository;
import com.silverline.erp.module.pos.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final AuthenticationManager authenticationManager;
    private final UserProfileRepo userProfileRepo;
    private final CashFlowRepository cashFlowRepository;
    private final AuditLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

    // --- 1. START SHIFT ---
    @Override
    @Transactional
    public Long startShift(ShiftStartRequest request) {
        Long supervisorId = null;
        LocalDateTime approvedAt = null;

        if (request.getSupervisorUsername() != null &&
                !request.getSupervisorUsername().isEmpty()) {

            String username = request.getSupervisorUsername();
            String password = request.getSupervisorPassword();

            if (password == null || password.isEmpty()) {
                throw new RuntimeException("Supervisor password is required");
            }

            try {
                UserProfile supervisor = userProfileRepo.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Supervisor user not found: " + username));

                if (supervisor.getAccountStatus() != AccountStatus.ACTIVE) {
                    throw new RuntimeException("Supervisor account is not active. Current status: " + supervisor.getAccountStatus());
                }

                Authentication auth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, password)
                );

                if (!auth.isAuthenticated()) {
                    throw new RuntimeException("Invalid supervisor credentials");
                }

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
            }
        }

        if (shiftRepository.hasOpenShift(request.getCashierId())) {
            throw new IllegalStateException("Cashier already has an active shift.");
        }

        CashShift shift = new CashShift();
        shift.setShiftNo(generateShiftNo(request.getBranchId()));
        shift.setBranchId(request.getBranchId());
        shift.setCashierId(request.getCashierId());
        shift.setOpeningCash(request.getOpeningCash());
        shift.setOpenedAt(LocalDateTime.now());
        shift.setStatus(CashShift.ShiftStatus.valueOf("OPEN"));
        shift.setApprovedBy(supervisorId);
        shift.setApprovedAt(approvedAt);

        shift.setTotalSales(BigDecimal.ZERO);
        shift.setTotalReturns(BigDecimal.ZERO);
        shift.setClosingCash(BigDecimal.ZERO);
        shift.setExpectedCash(request.getOpeningCash());
        shift.setCashDifference(BigDecimal.ZERO);

        Long savedShiftId = shiftRepository.save(shift);
        shift.setShiftId(savedShiftId);
     
        String cashierUsername = userProfileRepo.findById(request.getCashierId())
                .map(UserProfile::getUsername)
                .orElse("Cashier #" + request.getCashierId());

        activityLogService.logActivity(
            request.getBranchId(),
            null,
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
    @Override
    public Long getActiveShiftId(Long cashierId) {
        return shiftRepository.findOpenShiftByCashierId(cashierId)
                .map(CashShift::getShiftId)
                .orElseThrow(() -> new IllegalStateException("No active shift found for this cashier. Please open a shift."));
    }

    // --- 3. CLOSE SHIFT ---
    @Override
    @Transactional
    public void closeShift(Long cashierId, CloseShiftRequest request) {
        CashShift shift = shiftRepository.findOpenShiftByCashierId(cashierId)
                .orElseThrow(() -> new IllegalStateException("No open shift found for this cashier"));
        
        long pending = cashFlowRepository.countByShiftIdAndStatus(shift.getShiftId(), "PENDING");
        if (pending > 0) {
            throw new IllegalStateException("Cannot close shift with " + pending + " pending cash flow requests. waiting for approval.");
        }

        performCloseShift(shift, request);
    }

    @Override
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

        String cashierUsername = userProfileRepo.findById(shift.getCashierId())
                .map(UserProfile::getUsername)
                .orElse("Cashier #" + shift.getCashierId());

        // Publish ShiftClosedEvent to log activity asynchronously
        eventPublisher.publishEvent(new ShiftClosedEvent(
                shift.getShiftId(),
                shift.getShiftNo(),
                shift.getBranchId(),
                shift.getCashierId(),
                cashierUsername,
                request.getClosingCash(),
                expected,
                shift.getCashDifference(),
                request.getNotes()
        ));
    }

    // --- 4. GET ACTIVE SHIFT ---
    @Override
    public ShiftResponse getActiveShift(Long branchId, Long cashierId) {
        CashShift shift = null;

        if (cashierId != null) {
            shift = shiftRepository.findOpenShiftByCashierId(cashierId)
                    .map(s -> shiftRepository.findByIdWithStats(s.getShiftId()).orElse(s))
                    .orElse(null);
        }

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

        return new ShiftResponse.Builder()
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

    // --- 5. GET CASHIERS ---
    @Override
    public List<UserProfile> getCashiersByBranch(Long branchId) {
        return userProfileRepo.findByRole(Role.CASHIER);
    }

    private String generateShiftNo(Long branchId) {
        return "SH-" + branchId + "-" + System.currentTimeMillis();
    }
}
