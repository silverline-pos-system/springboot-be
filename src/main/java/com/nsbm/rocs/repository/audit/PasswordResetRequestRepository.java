package com.nsbm.rocs.repository.audit;

import com.nsbm.rocs.entity.audit.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    List<PasswordResetRequest> findByStatus(String status);
    List<PasswordResetRequest> findByUserId(Long userId);
    List<PasswordResetRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<PasswordResetRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
