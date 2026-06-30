package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.SupplierContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierContactRepository extends JpaRepository<SupplierContact, Long> {
}


