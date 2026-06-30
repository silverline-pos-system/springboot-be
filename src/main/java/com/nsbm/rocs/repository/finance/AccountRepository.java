package com.nsbm.rocs.repository.finance;

import com.nsbm.rocs.entity.finance.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByCode(String code);
    List<Account> findByType(String type);
    List<Account> findByParentId(Long parentId);
    List<Account> findByIsActiveTrue();
}

