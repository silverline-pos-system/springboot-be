package com.silverline.erp.module.inventory.service;

import com.silverline.erp.module.inventory.dto.ExpiryAlertDTO;

import java.time.LocalDate;
import java.util.List;

public interface ExpiryCalendarService {

    List<ExpiryAlertDTO> getExpiryCalendar(LocalDate start, LocalDate end, Long branchId);

    List<ExpiryAlertDTO> getExpiringSoon(Long branchId, Integer daysAhead);

    List<ExpiryAlertDTO> getExpired(Long branchId);
}


