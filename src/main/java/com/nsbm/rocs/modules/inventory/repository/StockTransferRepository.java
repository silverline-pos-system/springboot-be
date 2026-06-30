package com.nsbm.rocs.modules.inventory.repository;

import com.nsbm.rocs.entity.inventory.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
}

