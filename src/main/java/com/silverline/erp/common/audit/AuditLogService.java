package com.silverline.erp.common.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.silverline.erp.common.audit.repository.BranchActivityRepository;
import com.silverline.erp.domain.audit.BranchActivity;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuditLogService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private BranchActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Log an activity. terminalId parameter removed â€” pass null from all callers.
     * Overloaded method kept for backward compatibility but ignores terminalId.
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void logActivity(Long branchId, Long terminalId, Long userId, String username, String userRole,
                            String actionType, String entityType, Long entityId, String details, String metadata) {
        // terminalId is IGNORED â€” kept in signature for backward compat during migration
        logActivity(branchId, userId, username, userRole, actionType, entityType, entityId, details, metadata);
    }

    /**
     * Primary activity logging method â€” no terminalId.
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void logActivity(Long branchId, Long userId, String username, String userRole,
                            String actionType, String entityType, Long entityId, String details, String metadata) {
        try {
            String safeMetadata = buildSafeMetadata(metadata, username, userRole);

            BranchActivity activity = BranchActivity.builder()
                    .branchId(branchId != null ? branchId : 1L)
                    .actionType(actionType)
                    .userId(userId)
                    .details(details)
                    .entityType(entityType)
                    .entityId(entityId)
                    .metadata(safeMetadata)
                    .severity("INFO")
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();

            activityRepository.save(activity);
        } catch (Exception e) {
            log.error("Failed to save activity log: {}", e.getMessage(), e);
        }
    }

    private String buildSafeMetadata(String metadata, String username, String userRole) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();

        String trimmed = metadata == null ? "" : metadata.trim();
        if (!trimmed.isEmpty()) {
            try {
                JsonNode parsed = OBJECT_MAPPER.readTree(trimmed);
                if (parsed != null && parsed.isObject()) {
                    root.setAll((ObjectNode) parsed);
                } else {
                    root.set("value", parsed);
                }
            } catch (Exception ignored) {
                // Metadata came as non-JSON text (e.g. key=value map toString), store safely as string.
                root.put("info", metadata);
            }
        }

        if (username != null || userRole != null) {
            ObjectNode userInfo = OBJECT_MAPPER.createObjectNode();
            userInfo.put("user", username != null ? username : "");
            userInfo.put("role", userRole != null ? userRole : "");
            root.set("user_info", userInfo);
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    public List<BranchActivity> getRecentActivities(Long branchId) {
        return enrichActivitiesWithUserInfo(activityRepository.findTop20ByBranchIdOrderByTimestampDesc(branchId));
    }

    public List<BranchActivity> getActivitiesByDateRange(Long branchId, LocalDateTime start, LocalDateTime end) {
        return enrichActivitiesWithUserInfo(activityRepository.findByBranchIdAndTimestampBetweenOrderByTimestampDesc(branchId, start, end));
    }

    private List<BranchActivity> enrichActivitiesWithUserInfo(List<BranchActivity> activities) {
        Set<Long> userIds = activities.stream()
                .map(activity -> activity.getUserId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return activities;
        }

        Map<Long, UserProfile> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(u -> u.getUserId(), u -> u));

        activities.forEach(activity -> {
            Long userId = activity.getUserId();
            if (userId == null) {
                return;
            }
            UserProfile user = userMap.get(userId);
            if (user != null) {
                activity.setUsername(user.getUsername());
                activity.setUserRole(user.getRole() != null ? user.getRole().name() : null);
            }
        });

        return activities;
    }
}


