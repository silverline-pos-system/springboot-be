package com.nsbm.rocs.modules.pos.repository;

import com.nsbm.rocs.entity.pos.Payment;
import java.util.List;

public interface PaymentRepositoryCustom {
    void saveBatch(List<Payment> payments);
}


