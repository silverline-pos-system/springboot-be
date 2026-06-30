package com.silverline.erp.module.pos.service;

import com.silverline.erp.domain.inventory.Product;
import com.silverline.erp.domain.pos.QuickPickItem;
import com.silverline.erp.module.pos.repository.QuickPickRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuickPickService {

    private final QuickPickRepository quickPickRepository;
    private final ProductRepository productRepository;

    public List<Product> getQuickPickProducts(Long branchId) {
        List<QuickPickItem> items = quickPickRepository.findByBranchId(branchId);
        List<Long> productIds = items.stream()
                .map(QuickPickItem::getProductId)
                .collect(Collectors.toList());
        
        return productRepository.findAllById(productIds);
    }

    @Transactional
    public void addItem(Long branchId, Long productId) {
        if (!quickPickRepository.findByBranchIdAndProductId(branchId, productId).isPresent()) {
            QuickPickItem item = new QuickPickItem();
            item.setBranchId(branchId);
            item.setProductId(productId);
            quickPickRepository.save(item);
        }
    }

    @Transactional
    public void removeItem(Long branchId, Long productId) {
        quickPickRepository.deleteByBranchIdAndProductId(branchId, productId);
    }
}

