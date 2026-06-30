package com.nsbm.rocs.repository.audit;

import com.nsbm.rocs.entity.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByBranchId(Long branchId);
    List<AuditLog> findByTableName(String tableName);
    List<AuditLog> findByRecordId(Long recordId);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}

