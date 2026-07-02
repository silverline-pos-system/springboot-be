package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.procurement.SupplierContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierContactRepository extends JpaRepository<SupplierContact, Long> {
}


