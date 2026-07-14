package com.silverline.erp.module.manager.repository;

import com.silverline.erp.domain.user.SecondaryRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SecondaryRoleAssignmentRepository extends JpaRepository<SecondaryRoleAssignment, Long> {

    List<SecondaryRoleAssignment> findAllByOrderByCreatedAtDesc();

    Optional<SecondaryRoleAssignment> findFirstByUserIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId, LocalDateTime now
    );

    boolean existsByUserIdAndRevokedFalseAndExpiresAtAfter(Long userId, LocalDateTime now);
}


