package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.LoyaltyOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LoyaltyOtpRepository extends JpaRepository<LoyaltyOtp, Long> {

    Optional<LoyaltyOtp> findTopByCustomerIdAndExpiresAtAfterOrderByCreatedAtDesc(Long customerId, LocalDateTime time);

    @Modifying
    @Query("DELETE FROM LoyaltyOtp o WHERE o.customerId = :customerId")
    void deleteByCustomerId(Long customerId);

    @Modifying
    @Query("DELETE FROM LoyaltyOtp o WHERE o.expiresAt < :time")
    int deleteByExpiresAtBefore(LocalDateTime time);
}
