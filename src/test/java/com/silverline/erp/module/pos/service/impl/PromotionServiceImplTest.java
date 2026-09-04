package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.domain.pos.PromoType;
import com.silverline.erp.domain.pos.Promotion;
import com.silverline.erp.module.inventory.repository.BranchProductRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.pos.dto.PromotionEval;
import com.silverline.erp.module.pos.repository.PromotionRepository;
import com.silverline.erp.module.pos.repository.PromotionUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionUsageRepository promotionUsageRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BranchProductRepository branchProductRepository;

    @InjectMocks private PromotionServiceImpl service;

    private Promotion bogo() {
        Promotion p = new Promotion();
        p.setPromotionId(1L);
        p.setName("Buy 3 Get 1 Free");
        p.setPromoType(PromoType.BUY_X_GET_Y_FREE);
        p.setScopeType("PRODUCT");
        p.setScopeRefId(10L);
        p.setBuyQty(new BigDecimal("3"));
        p.setGetQty(new BigDecimal("1"));
        p.setStackable(true);
        return p;
    }

    private Promotion percentOff() {
        Promotion p = new Promotion();
        p.setPromotionId(2L);
        p.setName("10% Off");
        p.setPromoType(PromoType.PERCENT_OFF);
        p.setScopeType("ALL");
        p.setDiscountPercent(new BigDecimal("10"));
        p.setStackable(true);
        return p;
    }

    @Test
    void buyThreeGetOneFree_addsOneFreeUnit() {
        when(promotionRepository.findActiveForBranch(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(bogo()));
        lenient().when(productRepository.findById(any())).thenReturn(Optional.empty());

        // 4 yogurts at 100 -> one group of 3 -> 1 free
        PromotionEval.Line line = new PromotionEval.Line(10L, new BigDecimal("4"), new BigDecimal("100"), null);
        PromotionEval.Outcome out = service.evaluate(List.of(line), 1L, LocalDateTime.now());

        assertEquals(1, out.getFreeItems().size());
        assertEquals(0, out.getFreeItems().get(0).getQty().compareTo(BigDecimal.ONE));
        assertEquals(10L, out.getFreeItems().get(0).getProductId());
        assertEquals(1, out.getApplied().size());
    }

    @Test
    void percentOff_appliesLineDiscount() {
        when(promotionRepository.findActiveForBranch(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(percentOff()));
        lenient().when(productRepository.findById(any())).thenReturn(Optional.empty());

        // 2 items at 250 -> subtotal 500 -> 10% = 50 discount
        PromotionEval.Line line = new PromotionEval.Line(10L, new BigDecimal("2"), new BigDecimal("250"), null);
        PromotionEval.Outcome out = service.evaluate(List.of(line), 1L, LocalDateTime.now());

        assertEquals(1, out.getLineDiscounts().size());
        assertEquals(0, out.getLineDiscounts().get(0).getDiscount().compareTo(new BigDecimal("50.00")));
    }

    @Test
    void multiplePromosStack() {
        when(promotionRepository.findActiveForBranch(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(bogo(), percentOff()));
        lenient().when(productRepository.findById(any())).thenReturn(Optional.empty());

        PromotionEval.Line line = new PromotionEval.Line(10L, new BigDecimal("3"), new BigDecimal("100"), null);
        PromotionEval.Outcome out = service.evaluate(List.of(line), 1L, LocalDateTime.now());

        // BOGO gives 1 free, percent-off gives a line discount -> both applied
        assertEquals(2, out.getApplied().size());
        assertTrue(out.getFreeItems().size() >= 1);
        assertTrue(out.getLineDiscounts().size() >= 1);
    }
}
