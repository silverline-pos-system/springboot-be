package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByCode(String code);

    boolean existsByPhone(String phone);

    List<Customer> findByNameContainingIgnoreCase(String name);

    List<Customer> findByPhoneContaining(String phone);

    List<Customer> findByIsActiveTrue();

    @org.springframework.data.jpa.repository.Query("SELECT SUM(c.loyaltyPoints) FROM Customer c")
    Long sumTotalLoyaltyPoints();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = true")
    Long countActiveCustomers();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = false")
    Long countInactiveCustomers();
}

