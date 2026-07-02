package com.silverline.erp.integration;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.pos.CashShift;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.pos.dto.ShiftStartRequest;
import com.silverline.erp.module.pos.dto.shift.CloseShiftRequest;
import com.silverline.erp.module.pos.repository.ShiftRepository;
import com.silverline.erp.module.pos.service.ShiftService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class ShiftLifecycleIntegrationTest {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    public void testShiftLifecycle_OpenClose_Success() {
        // 1. Create and persist Branch
        Branch branch = new Branch();
        branch.setName("Shift Test Branch");
        branch.setCode("BR_SHIFT_001");
        branch.setIsActive(true);
        branch = branchRepository.save(branch);

        // 2. Create and persist Cashier User
        UserProfile cashier = new UserProfile();
        cashier.setUsername("cashierShift");
        cashier.setEmail("cashierShift@example.com");
        cashier.setPassword("password");
        cashier.setFullName("Shift Cashier");
        cashier.setEmployeeId("EMP_CS_001");
        cashier.setRole(Role.CASHIER);
        cashier.setAccountStatus(AccountStatus.ACTIVE);
        cashier = userProfileRepository.save(cashier);

        // 3. Prepare ShiftStartRequest (openingCash = 150.00)
        ShiftStartRequest startRequest = new ShiftStartRequest();
        startRequest.setBranchId(branch.getBranchId());
        startRequest.setCashierId(cashier.getUserId());
        startRequest.setOpeningCash(BigDecimal.valueOf(150.00));

        // 4. Start Shift
        Long shiftId = shiftService.startShift(startRequest);
        assertNotNull(shiftId);

        // 5. Verify active shift status is OPEN
        CashShift openShift = shiftRepository.findById(shiftId).orElseThrow();
        assertEquals(CashShift.ShiftStatus.OPEN, openShift.getStatus());
        assertEquals(0, BigDecimal.valueOf(150.00).compareTo(openShift.getOpeningCash()));

        // 6. Close Shift (closingCash = 500.00)
        CloseShiftRequest closeRequest = new CloseShiftRequest();
        closeRequest.setClosingCash(BigDecimal.valueOf(500.00));
        closeRequest.setNotes("End of day close");
        shiftService.closeShift(cashier.getUserId(), closeRequest);

        // 7. Verify closed shift expected stats
        CashShift closedShift = shiftRepository.findById(shiftId).orElseThrow();
        assertEquals(CashShift.ShiftStatus.CLOSED, closedShift.getStatus());
        assertEquals(0, BigDecimal.valueOf(500.00).compareTo(closedShift.getClosingCash()));
        assertEquals(0, BigDecimal.valueOf(150.00).compareTo(closedShift.getExpectedCash())); // No transactions occurred
        assertEquals(0, BigDecimal.valueOf(350.00).compareTo(closedShift.getCashDifference())); // 500 - 150
    }
}
