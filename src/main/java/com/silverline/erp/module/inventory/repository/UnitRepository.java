package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.inventory.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByName(String name);

    boolean existsByName(String name);
}


