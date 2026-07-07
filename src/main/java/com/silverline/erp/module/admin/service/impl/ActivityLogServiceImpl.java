package com.silverline.erp.module.admin.service.impl;

import com.silverline.erp.common.audit.repository.BranchActivityRepository;
import com.silverline.erp.domain.audit.BranchActivity;
import com.silverline.erp.domain.audit.UserActivityLog;
import com.silverline.erp.module.admin.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ActivityLogServiceImpl implements ActivityLogService {

    private final BranchActivityRepository logRepository;

    private UserActivityLog mapToUserActivityLog(BranchActivity activity) {
        if (activity == null) {
            return null;
        }
        UserActivityLog log = new UserActivityLog();
        log.setActivityId(activity.getId());
        log.setBranchId(activity.getBranchId());
        log.setUserId(activity.getUserId());
        log.setPerformedBy(activity.getUserId());
        log.setActivityType(activity.getActionType());
        log.setDescription(activity.getDetails());
        log.setIpAddress(activity.getIpAddress());
        log.setUserAgent(activity.getUserAgent());
        log.setCreatedAt(activity.getTimestamp());
        return log;
    }

    @Override
    public List<UserActivityLog> getAllLogs() {
        return logRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
                .stream()
                .map(this::mapToUserActivityLog)
                .toList();
    }

    @Override
    public List<UserActivityLog> getLogsByFilter(Long branchId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        boolean hasType = type != null && !type.isEmpty();
        boolean hasDates = startDate != null && endDate != null;
        boolean hasBranch = branchId != null;

        List<BranchActivity> activities;

        if (hasBranch && hasType && hasDates) {
            activities = logRepository.findByBranchIdAndActionTypeAndTimestampBetween(branchId, type, startDate, endDate);
        } else if (hasBranch && hasType) {
            activities = logRepository.findByBranchIdAndActionType(branchId, type);
        } else if (hasBranch && hasDates) {
            activities = logRepository.findByBranchIdAndTimestampBetween(branchId, startDate, endDate);
        } else if (hasBranch) {
            activities = logRepository.findByBranchId(branchId);
        } else if (hasType && hasDates) {
            activities = logRepository.findByActionTypeAndTimestampBetween(type, startDate, endDate);
        } else if (hasType) {
            activities = logRepository.findByActionType(type);
        } else if (hasDates) {
            activities = logRepository.findByTimestampBetween(startDate, endDate);
        } else {
            return getAllLogs();
        }

        return activities.stream()
                .map(this::mapToUserActivityLog)
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();
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


