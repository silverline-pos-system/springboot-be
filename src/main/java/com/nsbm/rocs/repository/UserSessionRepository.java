package com.nsbm.rocs.repository;

import com.nsbm.rocs.entity.main.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByToken(String token);
    List<UserSession> findByUserId(Long userId);
    List<UserSession> findByUserIdAndIsActiveTrue(Long userId);

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.userId = :userId")
    void deactivateAllUserSessions(Long userId);

    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :now")
    void deleteExpiredSessions(LocalDateTime now);
}

