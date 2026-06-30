package com.silverline.erp.module.pos.repository;

import com.silverline.erp.domain.pos.Payment;
import java.util.List;

public interface PaymentRepositoryCustom {
    void saveBatch(List<Payment> payments);
}


