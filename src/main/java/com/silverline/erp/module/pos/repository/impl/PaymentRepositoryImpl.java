package com.silverline.erp.module.pos.repository.impl;

import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.module.pos.repository.PaymentRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveBatch(List<Payment> payments) {
        String sql = "INSERT INTO payments " +
                "(sale_id, payment_type, amount, reference_no, card_last4, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                Payment payment = payments.get(i);
                ps.setLong(1, payment.getSaleId());
                ps.setString(2, payment.getPaymentType());
                ps.setBigDecimal(3, payment.getAmount());
                ps.setString(4, payment.getReferenceNo());
                ps.setString(5, payment.getCardLast4());
            }

            @Override
            public int getBatchSize() {
                return payments.size();
            }
        });
    }
}


