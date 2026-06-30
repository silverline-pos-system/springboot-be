package com.nsbm.rocs.modules.admin.service;

import com.nsbm.rocs.entity.audit.UserActivityLog;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogService {
    List<UserActivityLog> getAllLogs();

    List<UserActivityLog> getLogsByFilter(Long branchId, String type, LocalDateTime startDate, LocalDateTime endDate);

    List<UserActivityLog> searchLogs(String query, Long branchId, String type, LocalDateTime startDate, LocalDateTime endDate);
}

