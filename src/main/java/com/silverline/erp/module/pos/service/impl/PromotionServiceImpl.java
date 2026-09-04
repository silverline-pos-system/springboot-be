package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.domain.inventory.BranchProduct;
import com.silverline.erp.domain.pos.PromoType;
import com.silverline.erp.domain.pos.Promotion;
import com.silverline.erp.domain.pos.PromotionUsage;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.repository.BranchProductRepository;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.pos.dto.PromotionEval;
import com.silverline.erp.module.pos.repository.PromotionRepository;
import com.silverline.erp.module.pos.repository.PromotionUsageRepository;
import com.silverline.erp.module.pos.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;

    @Override
    public PromotionEval.Outcome evaluate(List<PromotionEval.Line> lines, Long branchId, LocalDateTime now) {
        PromotionEval.Outcome out = new PromotionEval.Outcome();
        if (lines == null || lines.isEmpty()) return out;

        // Resolve each line's category once.
        Map<Integer, Long> lineCategory = new HashMap<>();
        BigDecimal cartSubtotal = BigDecimal.ZERO;
        for (int i = 0; i < lines.size(); i++) {
            PromotionEval.Line l = lines.get(i);
            lineCategory.put(i, categoryOf(l.getProductId()));
            cartSubtotal = cartSubtotal.add(safe(l.getUnitPrice()).multiply(safe(l.getQty())));
        }

        List<Promotion> promos = promotionRepository.findActiveForBranch(branchId, now);
        boolean exclusiveApplied = false;

        for (Promotion p : promos) {
            if (exclusiveApplied) break; // an exclusive (non-stackable) promo already won
            if (Boolean.FALSE.equals(p.getStackable()) && !out.getApplied().isEmpty()) continue;

            if (p.getMinCartAmount() != null && cartSubtotal.compareTo(p.getMinCartAmount()) < 0) continue;

            BigDecimal benefit = apply(p, lines, lineCategory, branchId, now, out);
            if (benefit.compareTo(BigDecimal.ZERO) > 0) {
                out.getApplied().add(new PromotionEval.Applied(
                        p.getPromotionId(), p.getName(), p.getPromoType().name(), round(benefit)));
                if (Boolean.FALSE.equals(p.getStackable())) {
                    exclusiveApplied = true;
                }
            }
        }
        return out;
    }

    private BigDecimal apply(Promotion p, List<PromotionEval.Line> lines, Map<Integer, Long> lineCategory,
                             Long branchId, LocalDateTime now, PromotionEval.Outcome out) {
        BigDecimal benefit = BigDecimal.ZERO;

        switch (p.getPromoType()) {
            case PERCENT_OFF -> {
                for (int i = 0; i < lines.size(); i++) {
                    if (!qualifies(p, lines.get(i), lineCategory.get(i))) continue;
                    PromotionEval.Line l = lines.get(i);
                    BigDecimal d = round(safe(l.getUnitPrice()).multiply(safe(l.getQty()))
                            .multiply(pct(p.getDiscountPercent())));
                    if (d.compareTo(BigDecimal.ZERO) > 0) {
                        out.getLineDiscounts().add(new PromotionEval.LineDiscount(
                                i, d, p.getPromotionId(), p.getName()));
                        benefit = benefit.add(d);
                    }
                }
            }
            case AMOUNT_OFF -> {
                BigDecimal qualifyingSubtotal = BigDecimal.ZERO;
                for (int i = 0; i < lines.size(); i++) {
                    if (qualifies(p, lines.get(i), lineCategory.get(i))) {
                        PromotionEval.Line l = lines.get(i);
                        qualifyingSubtotal = qualifyingSubtotal.add(safe(l.getUnitPrice()).multiply(safe(l.getQty())));
                    }
                }
                if (qualifyingSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal d = round(safe(p.getDiscountAmount()).min(qualifyingSubtotal));
                    out.setCartDiscount(out.getCartDiscount().add(d));
                    benefit = benefit.add(d);
                }
            }
            case BUY_X_GET_Y_FREE -> {
                BigDecimal buy = safe(p.getBuyQty());
                BigDecimal get = safe(p.getGetQty());
                if (buy.compareTo(BigDecimal.ZERO) > 0 && get.compareTo(BigDecimal.ZERO) > 0) {
                    for (int i = 0; i < lines.size(); i++) {
                        if (!qualifies(p, lines.get(i), lineCategory.get(i))) continue;
                        PromotionEval.Line l = lines.get(i);
                        long groups = safe(l.getQty()).divideToIntegralValue(buy).longValueExact();
                        if (groups <= 0) continue;
                        BigDecimal freeUnits = get.multiply(BigDecimal.valueOf(groups));
                        Long freeProduct = p.getGetProductId() != null ? p.getGetProductId() : l.getProductId();
                        BigDecimal freePrice = freeProduct.equals(l.getProductId())
                                ? safe(l.getUnitPrice()) : priceOf(freeProduct, branchId);
                        BigDecimal value = round(freePrice.multiply(freeUnits));
                        out.getFreeItems().add(new PromotionEval.FreeItem(
                                freeProduct, freeUnits, freePrice, p.getPromotionId(), p.getName()));
                        benefit = benefit.add(value);
                    }
                }
            }
            case N_FOR_FIXED -> {
                BigDecimal buy = safe(p.getBuyQty());
                if (buy.compareTo(BigDecimal.ZERO) > 0 && p.getFixedPrice() != null) {
                    for (int i = 0; i < lines.size(); i++) {
                        if (!qualifies(p, lines.get(i), lineCategory.get(i))) continue;
                        PromotionEval.Line l = lines.get(i);
                        long groups = safe(l.getQty()).divideToIntegralValue(buy).longValueExact();
                        if (groups <= 0) continue;
                        BigDecimal normal = safe(l.getUnitPrice()).multiply(buy);
                        BigDecimal save = normal.subtract(p.getFixedPrice()).multiply(BigDecimal.valueOf(groups));
                        if (save.compareTo(BigDecimal.ZERO) > 0) {
                            save = round(save);
                            out.getLineDiscounts().add(new PromotionEval.LineDiscount(
                                    i, save, p.getPromotionId(), p.getName()));
                            benefit = benefit.add(save);
                        }
                    }
                }
            }
            case BUNDLE -> {
                // Basic bundle: scope product + getProductId both present -> fixed combo price.
                Integer aIdx = firstLineOfProduct(lines, p.getScopeRefId());
                Integer bIdx = p.getGetProductId() != null ? firstLineOfProduct(lines, p.getGetProductId()) : null;
                if (aIdx != null && bIdx != null && p.getFixedPrice() != null) {
                    BigDecimal comboNormal = safe(lines.get(aIdx).getUnitPrice()).add(safe(lines.get(bIdx).getUnitPrice()));
                    BigDecimal save = round(comboNormal.subtract(p.getFixedPrice()));
                    if (save.compareTo(BigDecimal.ZERO) > 0) {
                        out.setCartDiscount(out.getCartDiscount().add(save));
                        benefit = benefit.add(save);
                    }
                }
            }
            case EXPIRY_CLEARANCE -> {
                int days = p.getClearanceDays() != null ? p.getClearanceDays() : 0;
                LocalDate today = now.toLocalDate();
                for (int i = 0; i < lines.size(); i++) {
                    PromotionEval.Line l = lines.get(i);
                    if (!qualifies(p, l, lineCategory.get(i))) continue;
                    if (l.getBatchExpiry() == null) continue;
                    long daysToExpiry = ChronoUnit.DAYS.between(today, l.getBatchExpiry());
                    if (daysToExpiry < 0 || daysToExpiry > days) continue;
                    BigDecimal d = round(safe(l.getUnitPrice()).multiply(safe(l.getQty()))
                            .multiply(pct(p.getDiscountPercent())));
                    if (d.compareTo(BigDecimal.ZERO) > 0) {
                        out.getLineDiscounts().add(new PromotionEval.LineDiscount(
                                i, d, p.getPromotionId(), p.getName() + " (clearance)"));
                        benefit = benefit.add(d);
                    }
                }
            }
        }
        return benefit;
    }

    @Override
    @Transactional
    public void recordUsage(PromotionEval.Outcome outcome, Long saleId) {
        if (outcome == null) return;
        for (PromotionEval.Applied a : outcome.getApplied()) {
            PromotionUsage usage = new PromotionUsage();
            usage.setPromotionId(a.getPromotionId());
            usage.setSaleId(saleId);
            usage.setBenefitAmount(a.getBenefit());
            promotionUsageRepository.save(usage);

            promotionRepository.findById(a.getPromotionId()).ifPresent(p -> {
                p.setUsesCount((p.getUsesCount() == null ? 0 : p.getUsesCount()) + 1);
                promotionRepository.save(p);
            });
        }
    }

    // ---- helpers ----

    private boolean qualifies(Promotion p, PromotionEval.Line line, Long categoryId) {
        String scope = p.getScopeType() == null ? "ALL" : p.getScopeType();
        return switch (scope) {
            case "PRODUCT" -> p.getScopeRefId() != null && p.getScopeRefId().equals(line.getProductId());
            case "CATEGORY" -> p.getScopeRefId() != null && p.getScopeRefId().equals(categoryId);
            default -> true; // ALL
        };
    }

    private Integer firstLineOfProduct(List<PromotionEval.Line> lines, Long productId) {
        if (productId == null) return null;
        for (int i = 0; i < lines.size(); i++) {
            if (productId.equals(lines.get(i).getProductId())) return i;
        }
        return null;
    }

    private Long categoryOf(Long productId) {
        return productRepository.findById(productId).map(Product::getCategoryId).orElse(null);
    }

    private BigDecimal priceOf(Long productId, Long branchId) {
        if (branchId != null) {
            BigDecimal bp = branchProductRepository.findByBranchIdAndProductId(branchId, productId)
                    .map(BranchProduct::getSellingPrice).orElse(null);
            if (bp != null) return bp;
        }
        return productRepository.findById(productId).map(Product::getSellingPrice).orElse(BigDecimal.ZERO);
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal pct(BigDecimal percent) {
        return safe(percent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
