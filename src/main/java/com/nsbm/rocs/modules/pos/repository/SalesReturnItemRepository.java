package com.nsbm.rocs.modules.pos.repository;

import com.nsbm.rocs.entity.pos.SalesReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesReturnItemRepository extends JpaRepository<SalesReturnItem, Long> {
    List<SalesReturnItem> findBySalesReturn_ReturnId(Long returnId);
}

