package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByName(String name);

    boolean existsByName(String name);
}


