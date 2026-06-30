package com.silverline.erp.module.admin.service.impl;

import com.silverline.erp.module.admin.service.ActivityLogService;
import com.silverline.erp.domain.audit.UserActivityLog;
import com.silverline.erp.common.audit.repository.UserActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private final UserActivityLogRepository logRepository;

    @Autowired
    public ActivityLogServiceImpl(UserActivityLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public List<UserActivityLog> getAllLogs() {
        return logRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public List<UserActivityLog> getLogsByFilter(Long branchId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        boolean hasType = type != null && !type.isEmpty();
        boolean hasDates = startDate != null && endDate != null;
        boolean hasBranch = branchId != null;

        if (hasBranch && hasType && hasDates) {
            return logRepository.findByBranchIdAndActivityTypeAndCreatedAtBetween(branchId, type, startDate, endDate);
        } else if (hasBranch && hasType) {
            return logRepository.findByBranchIdAndActivityType(branchId, type);
        } else if (hasBranch && hasDates) {
            return logRepository.findByBranchIdAndCreatedAtBetween(branchId, startDate, endDate);
        } else if (hasBranch) {
            return logRepository.findByBranchId(branchId);
        } else if (hasType && hasDates) {
            return logRepository.findByActivityTypeAndCreatedAtBetween(type, startDate, endDate);
        } else if (hasType) {
            return logRepository.findByActivityType(type);
        } else if (hasDates) {
            return logRepository.findByCreatedAtBetween(startDate, endDate);
        } else {
            return getAllLogs();
        }
    }

    @Override
    public List<UserActivityLog> searchLogs(String query, Long branchId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        List<UserActivityLog> logs = getLogsByFilter(branchId, type, startDate, endDate);
        if (query == null || query.isEmpty()) {
            return logs;
        }
        String q = query.toLowerCase();
        return logs.stream()
                .filter(log -> (log.getDescription() != null && log.getDescription().toLowerCase().contains(q)) ||
                        (log.getActivityType() != null && log.getActivityType().toLowerCase().contains(q)) ||
                        (log.getUserId() != null && log.getUserId().toString().contains(q)))
                .toList();
    }
}

