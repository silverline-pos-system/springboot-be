package com.silverline.erp.module.inventory.repository;

import com.silverline.erp.domain.inventory.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
}

