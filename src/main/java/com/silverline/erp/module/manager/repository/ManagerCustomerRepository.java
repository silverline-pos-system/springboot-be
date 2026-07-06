package com.silverline.erp.module.manager.repository;

import com.silverline.erp.domain.pos.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerCustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByIsActiveTrue();

    @Query("SELECT SUM(c.loyaltyPoints) FROM Customer c")
    Long sumTotalLoyaltyPoints();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = true")
    Long countActiveCustomers();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = false")
    Long countInactiveCustomers();
}

