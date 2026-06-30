package com.silverline.erp.module.pos.repository.impl;

import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.module.pos.dto.sale.SaleItemResponse;
import com.silverline.erp.module.pos.repository.SaleItemRepositoryCustom;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@AllArgsConstructor
public class SaleItemRepositoryImpl implements SaleItemRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveBatch(List<SaleItem> saleItems) {
        String sql = "INSERT INTO sale_items " +
                "(sale_id, product_id, serial_id, qty, unit_price, discount, total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                SaleItem item = saleItems.get(i);
                ps.setLong(1, item.getSaleId());
                ps.setLong(2, item.getProductId());

                if (item.getSerialId() != null) {
                    ps.setLong(3, item.getSerialId());
                } else {
                    ps.setNull(3, java.sql.Types.BIGINT);
                }

                ps.setBigDecimal(4, item.getQty());
                ps.setBigDecimal(5, item.getUnitPrice());
                ps.setBigDecimal(6, item.getDiscount());
                ps.setBigDecimal(7, item.getTotal());
            }

            @Override
            public int getBatchSize() {
                return saleItems.size();
            }
        });
    }

    @Override
    public List<SaleItemResponse> findBySaleIdWithProductDetails(Long saleId) {

        String sql = "SELECT si.*, p.name as product_name, p.sku, p.barcode, ps.serial_no " +
                "FROM sale_items si " +
                "JOIN products p ON si.product_id = p.product_id " +
                "LEFT JOIN product_serials ps ON si.serial_id = ps.serial_id " +
                "WHERE si.sale_id = ?";

        return jdbcTemplate.query(sql, (rs, _) -> {
            SaleItemResponse dto = new SaleItemResponse();
            dto.setSaleItemId(rs.getLong("sale_item_id"));
            dto.setProductId(rs.getLong("product_id"));
            dto.setProductName(rs.getString("product_name"));
            dto.setSku(rs.getString("sku"));
            dto.setBarcode(rs.getString("barcode"));

            long serialId = rs.getLong("serial_id");
            if (!rs.wasNull()) {
                dto.setSerialId(serialId);
            }
            dto.setSerialNo(rs.getString("serial_no"));

            dto.setQuantity(rs.getBigDecimal("qty"));
            dto.setUnitPrice(rs.getBigDecimal("unit_price"));
            dto.setDiscount(rs.getBigDecimal("discount"));
            dto.setTotal(rs.getBigDecimal("total"));

            return dto;
        }, saleId);
    }
}

