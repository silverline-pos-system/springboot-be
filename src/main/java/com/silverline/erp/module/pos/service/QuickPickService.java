package com.silverline.erp.module.pos.service;

import com.silverline.erp.domain.product.Product;

import java.util.List;

public interface QuickPickService {
    List<Product> getQuickPickProducts(Long branchId);
    void addItem(Long branchId, Long productId);
    void removeItem(Long branchId, Long productId);
}
