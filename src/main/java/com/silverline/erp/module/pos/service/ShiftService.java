package com.silverline.erp.module.pos.service;

import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.pos.dto.ShiftStartRequest;
import com.silverline.erp.module.pos.dto.shift.CloseShiftRequest;
import com.silverline.erp.module.pos.dto.shift.ShiftResponse;

import java.util.List;

public interface ShiftService {
    Long startShift(ShiftStartRequest request);

    Long getActiveShiftId(Long cashierId);

    void closeShift(Long cashierId, CloseShiftRequest request);

    void closeShiftById(Long shiftId, CloseShiftRequest request);

    ShiftResponse getActiveShift(Long branchId, Long cashierId);

    List<UserProfile> getCashiersByBranch(Long branchId);
}
