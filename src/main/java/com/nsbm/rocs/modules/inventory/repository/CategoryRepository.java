package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByIsActiveTrue();

    Optional<Category> findByNameAndIsActiveTrue(String name);

    boolean existsByName(String name);
}


