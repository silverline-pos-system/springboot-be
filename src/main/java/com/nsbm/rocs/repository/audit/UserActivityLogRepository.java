package com.nsbm.rocs.repository.audit;

import com.nsbm.rocs.entity.audit.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    List<UserActivityLog> findByUserId(Long userId);

    List<UserActivityLog> findByBranchId(Long branchId);

    List<UserActivityLog> findByActivityType(String activityType);

    List<UserActivityLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<UserActivityLog> findByUserIdAndActivityType(Long userId, String activityType);

    List<UserActivityLog> findByActivityTypeAndCreatedAtBetween(String activityType, LocalDateTime startDate,
            LocalDateTime endDate);

    List<UserActivityLog> findByBranchIdAndActivityType(Long branchId, String activityType);

    List<UserActivityLog> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime startDate, LocalDateTime endDate);

    List<UserActivityLog> findByBranchIdAndActivityTypeAndCreatedAtBetween(Long branchId, String activityType,
            LocalDateTime startDate, LocalDateTime endDate);
}
