package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.pos.QuickPickItem;
import com.silverline.erp.module.pos.repository.QuickPickRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.pos.service.QuickPickService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuickPickServiceImpl implements QuickPickService {

    private final QuickPickRepository quickPickRepository;
    private final ProductRepository productRepository;

    @Override
    public List<Product> getQuickPickProducts(Long branchId) {
        List<QuickPickItem> items = quickPickRepository.findByBranchId(branchId);
        List<Long> productIds = items.stream()
                .map(QuickPickItem::getProductId)
                .collect(Collectors.toList());
        
        return productRepository.findAllById(productIds);
    }

    @Override
    @Transactional
    public void addItem(Long branchId, Long productId) {
        if (!quickPickRepository.findByBranchIdAndProductId(branchId, productId).isPresent()) {
            QuickPickItem item = new QuickPickItem();
            item.setBranchId(branchId);
            item.setProductId(productId);
            quickPickRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void removeItem(Long branchId, Long productId) {
        quickPickRepository.deleteByBranchIdAndProductId(branchId, productId);
    }
}
