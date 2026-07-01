package com.silverline.erp.module.admin.service;

import com.silverline.erp.domain.audit.UserActivityLog;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogService {
    List<UserActivityLog> getAllLogs();

    List<UserActivityLog> getLogsByFilter(Long branchId, String type, LocalDateTime startDate, LocalDateTime endDate);

    List<UserActivityLog> searchLogs(String query, Long branchId, String type, LocalDateTime startDate, LocalDateTime endDate);
}

