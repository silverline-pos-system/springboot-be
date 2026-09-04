package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.event.SaleCompletedEvent;
import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.admin.service.SaasFeatureService;
import com.silverline.erp.module.inventory.service.BatchService;
import com.silverline.erp.module.inventory.service.ProductSerialService;
import com.silverline.erp.module.inventory.service.ProductService;
import com.silverline.erp.module.inventory.service.StockService;
import com.silverline.erp.module.pos.dto.sale.CreateSaleRequest;
import com.silverline.erp.module.pos.dto.sale.PaymentRequest;
import com.silverline.erp.module.pos.dto.sale.SaleItemRequest;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.repository.PaymentRepository;
import com.silverline.erp.module.pos.repository.SaleItemRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.module.pos.service.PosSaleService;
import com.silverline.erp.module.pos.service.SaleQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PosSaleServiceImpl implements PosSaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductService productService;
    private final StockService stockService;
    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BatchService batchService;
    private final SaasFeatureService featureService;
    private final ProductSerialService productSerialService;
    private final SaleQueryService saleQueryService;
    private final com.silverline.erp.module.inventory.repository.BranchProductRepository branchProductRepository;
    private final com.silverline.erp.module.pos.service.PromotionService promotionService;

    // Statuses a client is allowed to request. Anything else is coerced to PAID so the client
    // cannot inject an arbitrary status to skip stock deduction or validation (mass-assignment guard).
    private static final java.util.Set<String> CLIENT_ALLOWED_STATUSES = java.util.Set.of("PAID", "HELD", "PENDING");

    @Override
    @Transactional
    public SaleResponse createSale(CreateSaleRequest request, Long branchId, Long cashierId, Long shiftId) {
        // Idempotent retry: if this exact checkout was already processed, return the original sale
        // instead of creating a duplicate (protects against lost-response network retries).
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            java.util.Optional<Sale> existing = saleRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent replay: returning existing sale {} for key {}", existing.get().getSaleId(), request.getIdempotencyKey());
                return saleQueryService.getSaleById(existing.get().getSaleId());
            }
        }

        String requestedStatus = normalizeStatus(request.getStatus());
        boolean isPaidSale = "PAID".equals(requestedStatus);

        log.info("Creating sale: branchId={}, cashierId={}, shiftId={}, status={}", branchId, cashierId, shiftId, requestedStatus);

        // --- IMEI/Serial Validation Gate ---
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            java.util.Set<Long> serialIdsInRequest = new java.util.HashSet<>();
            for (SaleItemRequest itemReq : request.getItems()) {
                if (itemReq.getSerialId() != null) {
                    if (!serialIdsInRequest.add(itemReq.getSerialId())) {
                        throw new com.silverline.erp.common.exception.ValidationException(
                            "Duplicate IMEI/Serial ID '" + itemReq.getSerialId() + "' scanned in the same transaction."
                        );
                    }

                    com.silverline.erp.module.inventory.dto.ProductSerialDTO serial = productSerialService.getSerialById(itemReq.getSerialId());
                    if (serial == null) {
                        throw new com.silverline.erp.common.exception.ResourceNotFoundException("Serial not found with ID: " + itemReq.getSerialId());
                    }
                    if (!"IN_STOCK".equals(serial.getStatus())) {
                        throw new com.silverline.erp.common.exception.ValidationException(
                            "Serial '" + serial.getSerialNo() + "' is already " + serial.getStatus().toLowerCase() + " and cannot be sold again."
                        );
                    }
                    if (!branchId.equals(serial.getBranchId())) {
                        throw new com.silverline.erp.common.exception.ValidationException(
                            "Serial '" + serial.getSerialNo() + "' belongs to another branch."
                        );
                    }
                    if (serial.getTransferId() != null) {
                        throw new com.silverline.erp.common.exception.ValidationException(
                            "Serial '" + serial.getSerialNo() + "' is currently locked in a stock transfer request."
                        );
                    }
                }
            }
        }

        if (isPaidSale && request.getItems() != null && !request.getItems().isEmpty()) {
            boolean allowOutOfStock = featureService.isFeatureEnabled("ALLOW_OUT_OF_STOCK");

            if (!allowOutOfStock) {
                for (SaleItemRequest itemReq : request.getItems()) {
                    BigDecimal requiredQty = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;

                    Product product = productService.findById(itemReq.getProductId());
                    String productName = product != null ? product.getName() : "Product #" + itemReq.getProductId();

                    if (itemReq.getProductId() == 332L || productName.toLowerCase().contains("service") || productName.toLowerCase().contains("dialog tv") || productName.toLowerCase().contains("dtv")) {
                        continue;
                    }

                    // Exact available quantity: getCurrentStock truncates fractional stock (0.75 -> 0),
                    // which would wrongly report a grocery item as out of stock.
                    BigDecimal availableQty = stockService.getCurrentStockExact(branchId, itemReq.getProductId());

                    if (availableQty.compareTo(BigDecimal.ZERO) <= 0) {
                        log.warn("Out of stock business rule violation: product='{}', branchId={}", productName, branchId);
                        throw new com.silverline.erp.common.exception.InsufficientStockException("Cannot sell '" + productName + "' — item is out of stock (Available: 0)");
                    }

                    if (availableQty.compareTo(requiredQty) < 0) {
                        log.warn("Insufficient stock business rule violation: product='{}', branchId={}, requested={}, available={}",
                                productName, branchId, requiredQty, availableQty);
                        throw new com.silverline.erp.common.exception.InsufficientStockException("Insufficient stock for '" + productName
                                + "'. Requested: " + requiredQty + ", Available: " + availableQty);
                    }
                }
            } else {
                log.info("Stock validation bypassed for sale due to ALLOW_OUT_OF_STOCK feature being active.");
            }
        }

        Sale sale = null;

        if (request.getSaleId() != null) {
            sale = saleRepository.findById(request.getSaleId()).orElse(null);
            if (sale != null && ("HELD".equalsIgnoreCase(sale.getPaymentStatus()) || "PENDING".equalsIgnoreCase(sale.getPaymentStatus()))) {
                saleItemRepository.deleteBySaleId(sale.getSaleId());
                paymentRepository.deleteBySaleId(sale.getSaleId());
            } else {
                sale = new Sale();
                sale.setInvoiceNo(generateInvoiceNo());
                sale.setSaleDate(LocalDateTime.now());
            }
        } else {
            sale = new Sale();
            sale.setInvoiceNo(generateInvoiceNo());
            sale.setSaleDate(LocalDateTime.now());
        }

        sale.setBranchId(branchId);
        sale.setCashierId(cashierId);
        sale.setCustomerId(request.getCustomerId());
        sale.setShiftId(shiftId);
        sale.setSaleDate(LocalDateTime.now());
        sale.setNotes(request.getNotes());
        sale.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        sale.setTaxAmount(BigDecimal.ZERO);
        sale.setSaleType(request.getSaleType() != null ? request.getSaleType() : "RETAIL");

        sale.setPaymentStatus(requestedStatus);
        sale.setIdempotencyKey(request.getIdempotencyKey());

        BigDecimal grossTotal = BigDecimal.ZERO;
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SaleItemRequest itemReq : request.getItems()) {
                Product product = productService.findById(itemReq.getProductId());
                com.silverline.erp.domain.inventory.Batch saleBatch = batchService
                        .resolveSaleBatch(branchId, itemReq.getProductId(), itemReq.getBatchId())
                        .orElse(null);
                BigDecimal unitPrice = resolveUnitPrice(itemReq, product, branchId, saleBatch);
                BigDecimal quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;

                BigDecimal lineGross = unitPrice.multiply(quantity);
                BigDecimal itemDiscount = clampDiscount(itemReq.getDiscount(), lineGross);
                grossTotal = grossTotal.add(lineGross.subtract(itemDiscount));
            }
        } else {
            if (request.getPayments() != null) {
                for (PaymentRequest pr : request.getPayments()) {
                    grossTotal = grossTotal.add(pr.getAmount() != null ? pr.getAmount() : BigDecimal.ZERO);
                }
            }
        }
        sale.setGrossTotal(grossTotal);

        // Cap the cart-level discount at the gross total so the net can never go negative (no negative sales).
        BigDecimal cartDiscount = clampDiscount(sale.getDiscount(), grossTotal);
        sale.setDiscount(cartDiscount);
        BigDecimal netTotal = grossTotal.subtract(cartDiscount);
        sale.setNetTotal(netTotal);

        BigDecimal paidAmount = BigDecimal.ZERO;
        if (request.getPayments() != null) {
            for (PaymentRequest pr : request.getPayments()) {
                BigDecimal amount = pr.getAmount() != null ? pr.getAmount() : BigDecimal.ZERO;
                paidAmount = paidAmount.add(amount);
            }
        }
        sale.setPaidAmount(paidAmount);
        sale.setChangeAmount(paidAmount.subtract(netTotal));

        if ("PAID".equals(sale.getPaymentStatus())) {
            if (paidAmount.compareTo(netTotal) < 0) {
                sale.setPaymentStatus("PARTIAL");
            }
        }

        Long saleId = saleRepository.save(sale);
        sale.setSaleId(saleId);

        List<SaleItem> saleItems = new ArrayList<>();
        List<com.silverline.erp.module.pos.dto.PromotionEval.Line> evalLines = new ArrayList<>();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SaleItemRequest itemReq : request.getItems()) {
                Product product = productService.findById(itemReq.getProductId());
                // FEFO default with cashier override: the priced batch is the chosen one
                // if valid, else the first-expired batch.
                com.silverline.erp.domain.inventory.Batch saleBatch = batchService
                        .resolveSaleBatch(branchId, itemReq.getProductId(), itemReq.getBatchId())
                        .orElse(null);
                BigDecimal unitPrice = resolveUnitPrice(itemReq, product, branchId, saleBatch);
                BigDecimal quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
                BigDecimal itemDiscount = clampDiscount(itemReq.getDiscount(), unitPrice.multiply(quantity));

                SaleItem item = new SaleItem();
                item.setSaleId(saleId);
                item.setProductId(itemReq.getProductId());
                item.setSerialId(itemReq.getSerialId());
                item.setBatchId(saleBatch != null ? saleBatch.getBatchId() : itemReq.getBatchId());
                item.setQty(quantity);
                item.setUnitPrice(unitPrice);
                item.setUnitCost(saleBatch != null ? saleBatch.getCostPrice()
                        : (product != null ? product.getCostPrice() : null));
                item.setDiscount(itemDiscount);
                item.setTotal(unitPrice.multiply(quantity));

                saleItemRepository.save(item);
                saleItems.add(item);
                evalLines.add(new com.silverline.erp.module.pos.dto.PromotionEval.Line(
                        itemReq.getProductId(), quantity, unitPrice,
                        saleBatch != null ? saleBatch.getExpiryDate() : null));
            }
        }

        // ---- Apply promotions authoritatively (line discounts, free items, cart discount) ----
        BigDecimal promoCartDiscount = BigDecimal.ZERO;
        if (!saleItems.isEmpty()) {
            com.silverline.erp.module.pos.dto.PromotionEval.Outcome promo =
                    promotionService.evaluate(evalLines, branchId, java.time.LocalDateTime.now());

            for (com.silverline.erp.module.pos.dto.PromotionEval.LineDiscount ld : promo.getLineDiscounts()) {
                if (ld.getLineIndex() < 0 || ld.getLineIndex() >= saleItems.size()) continue;
                SaleItem si = saleItems.get(ld.getLineIndex());
                BigDecimal maxDiscount = si.getUnitPrice().multiply(si.getQty());
                BigDecimal newDiscount = clampDiscount(
                        (si.getDiscount() != null ? si.getDiscount() : BigDecimal.ZERO).add(ld.getDiscount()), maxDiscount);
                si.setDiscount(newDiscount);
                si.setPromotionId(ld.getPromotionId());
                si.setDiscountReason(ld.getReason());
                saleItemRepository.save(si);
            }

            for (com.silverline.erp.module.pos.dto.PromotionEval.FreeItem fi : promo.getFreeItems()) {
                com.silverline.erp.domain.inventory.Batch freeBatch = batchService
                        .resolveSaleBatch(branchId, fi.getProductId(), null).orElse(null);
                BigDecimal lineValue = fi.getUnitPrice().multiply(fi.getQty());
                SaleItem free = new SaleItem();
                free.setSaleId(saleId);
                free.setProductId(fi.getProductId());
                free.setBatchId(freeBatch != null ? freeBatch.getBatchId() : null);
                free.setQty(fi.getQty());
                free.setUnitPrice(fi.getUnitPrice());
                free.setUnitCost(freeBatch != null ? freeBatch.getCostPrice() : null);
                free.setDiscount(lineValue); // fully discounted (free)
                free.setTotal(lineValue);
                free.setIsFree(true);
                free.setPromotionId(fi.getPromotionId());
                free.setDiscountReason(fi.getReason());
                saleItemRepository.save(free);
                saleItems.add(free);
            }

            promoCartDiscount = promo.getCartDiscount();
            promotionService.recordUsage(promo, saleId);
        }

        // ---- Recompute totals from the final sale items plus any cart-level discount ----
        BigDecimal itemsGross = BigDecimal.ZERO;
        BigDecimal itemsDiscount = BigDecimal.ZERO;
        for (SaleItem si : saleItems) {
            itemsGross = itemsGross.add(si.getUnitPrice().multiply(si.getQty()));
            itemsDiscount = itemsDiscount.add(si.getDiscount() != null ? si.getDiscount() : BigDecimal.ZERO);
        }
        BigDecimal finalCartDiscount = clampDiscount(
                (sale.getDiscount() != null ? sale.getDiscount() : BigDecimal.ZERO).add(promoCartDiscount),
                itemsGross.subtract(itemsDiscount).max(BigDecimal.ZERO));
        sale.setGrossTotal(itemsGross);
        sale.setDiscount(finalCartDiscount);
        BigDecimal finalNet = itemsGross.subtract(itemsDiscount).subtract(finalCartDiscount).max(BigDecimal.ZERO);
        sale.setNetTotal(finalNet);
        sale.setChangeAmount(sale.getPaidAmount() != null ? sale.getPaidAmount().subtract(finalNet) : BigDecimal.ZERO);
        if ("PAID".equals(sale.getPaymentStatus()) && sale.getPaidAmount() != null
                && sale.getPaidAmount().compareTo(finalNet) < 0) {
            sale.setPaymentStatus("PARTIAL");
        }
        saleRepository.save(sale);

        List<Payment> payments = new ArrayList<>();
        if (request.getPayments() != null) {
            for (PaymentRequest pr : request.getPayments()) {
                Payment payment = new Payment();
                payment.setSaleId(saleId);
                payment.setPaymentType(pr.getPaymentType() != null ? pr.getPaymentType() : "CASH");
                payment.setAmount(pr.getAmount() != null ? pr.getAmount() : BigDecimal.ZERO);
                payment.setReferenceNo(pr.getReferenceNo());
                payment.setCardLast4(pr.getCardLast4());
                payment.setBankName(pr.getBankName());

                paymentRepository.save(payment);
                payments.add(payment);
            }
        }

        final Sale finalSale = sale;
        if ("PAID".equals(sale.getPaymentStatus()) && !saleItems.isEmpty()) {
            for (SaleItem soldItem : saleItems) {
                try {
                    Product product = productService.findById(soldItem.getProductId());
                    String productName = product != null ? product.getName() : "";

                    if (!(soldItem.getProductId() == 332L || productName.toLowerCase().contains("service") || productName.toLowerCase().contains("dialog tv") || productName.toLowerCase().contains("dtv"))) {
                        BigDecimal soldQty = soldItem.getQty() != null ? soldItem.getQty() : BigDecimal.ONE;

                        if (soldItem.getSerialId() != null) {
                            // Serialized item: markAsSold performs the single, atomic stock decrement.
                            // Do NOT also call reduceStock here or the unit is deducted twice (DI-02).
                            productSerialService.markAsSold(soldItem.getSerialId(), finalSale.getSaleId());
                            log.info("Serial {} marked as SOLD for Sale {} (stock decremented once)", soldItem.getSerialId(), finalSale.getInvoiceNo());
                        } else {
                            // Non-serialized item: deduct the exact quantity. Pass BigDecimal so fractional
                            // quantities (e.g. 0.750 kg) are preserved instead of truncated to 0 (DI-03).
                            stockService.reduceStock(branchId, soldItem.getProductId(), soldQty);
                            log.info("Stock deducted for product {} in branch {}: -{}", soldItem.getProductId(), branchId, soldQty);
                        }

                        if (soldItem.getBatchId() != null) {
                            // Deduct the priced batch first, remainder oldest-first (FEFO).
                            batchService.deductForSale(branchId, soldItem.getProductId(), soldItem.getBatchId(), soldQty);
                            log.info("Batch stock deducted for product {} (preferred batch {}) by {}", soldItem.getProductId(), soldItem.getBatchId(), soldQty);
                        }
                    }
                } catch (Exception e) {
                    // Re-throw so the @Transactional sale rolls back. A paid sale must never commit
                    // with its stock movement missing (DI-04). Fail the checkout instead.
                    log.error("Stock deduction failed for product {} - rolling back sale {}", soldItem.getProductId(), finalSale.getInvoiceNo(), e);
                    throw new com.silverline.erp.common.exception.InsufficientStockException(
                            "Sale cancelled: could not update stock for product " + soldItem.getProductId() + ". " + e.getMessage());
                }
            }
        }

        UserProfile cashier = userProfileRepository.findById(cashierId)
                .orElseThrow(() -> new RuntimeException("Cashier profile not found"));
        String cashierUsername = cashier.getUsername();

        // Publish SaleCompletedEvent to log activity asynchronously
        eventPublisher.publishEvent(new SaleCompletedEvent(
                sale.getSaleId(),
                sale.getInvoiceNo(),
                branchId,
                cashierId,
                cashierUsername,
                sale.getNetTotal(),
                saleItems.size()
        ));

        return saleQueryService.mapToResponse(sale, saleItems, payments);
    }

    @Override
    @Transactional
    public void updateSaleStatus(Long saleId, String status) {
        saleRepository.findById(saleId).ifPresent(sale -> {
            sale.setPaymentStatus(status);
            saleRepository.save(sale);
        });
    }

    /**
     * Returns the price the server will actually charge for a line.
     * Physical goods: the catalog selling price is authoritative and the client-supplied price is ignored,
     * which blocks price manipulation (SEC-14). Service items (repairs, DTV installs) have no catalog price
     * and keep the POS-entered charge.
     */
    @Override
    public com.silverline.erp.module.pos.dto.CartPricing.Response priceCart(
            com.silverline.erp.module.pos.dto.CartPricing.Request request) {
        com.silverline.erp.module.pos.dto.CartPricing.Response resp =
                new com.silverline.erp.module.pos.dto.CartPricing.Response();
        Long branchId = request.getBranchId();
        if (request.getItems() == null || request.getItems().isEmpty()) return resp;

        List<com.silverline.erp.module.pos.dto.PromotionEval.Line> evalLines = new ArrayList<>();
        for (com.silverline.erp.module.pos.dto.CartPricing.Item it : request.getItems()) {
            Product product = productService.findById(it.getProductId());
            com.silverline.erp.domain.inventory.Batch saleBatch = batchService
                    .resolveSaleBatch(branchId, it.getProductId(), it.getBatchId()).orElse(null);
            BigDecimal qty = it.getQty() != null ? it.getQty() : BigDecimal.ONE;
            SaleItemRequest fake = new SaleItemRequest();
            fake.setProductId(it.getProductId());
            BigDecimal unitPrice = resolveUnitPrice(fake, product, branchId, saleBatch);

            com.silverline.erp.module.pos.dto.CartPricing.PricedLine line =
                    new com.silverline.erp.module.pos.dto.CartPricing.PricedLine(
                            it.getProductId(), product != null ? product.getName() : null,
                            saleBatch != null ? saleBatch.getBatchId() : it.getBatchId(),
                            qty, unitPrice, BigDecimal.ZERO, unitPrice.multiply(qty), false, null, null);
            resp.getItems().add(line);
            evalLines.add(new com.silverline.erp.module.pos.dto.PromotionEval.Line(
                    it.getProductId(), qty, unitPrice, saleBatch != null ? saleBatch.getExpiryDate() : null));
        }

        com.silverline.erp.module.pos.dto.PromotionEval.Outcome promo =
                promotionService.evaluate(evalLines, branchId, java.time.LocalDateTime.now());

        for (com.silverline.erp.module.pos.dto.PromotionEval.LineDiscount ld : promo.getLineDiscounts()) {
            if (ld.getLineIndex() < 0 || ld.getLineIndex() >= resp.getItems().size()) continue;
            com.silverline.erp.module.pos.dto.CartPricing.PricedLine l = resp.getItems().get(ld.getLineIndex());
            l.setLineDiscount(l.getLineDiscount().add(ld.getDiscount()));
            l.setLineTotal(l.getUnitPrice().multiply(l.getQty()).subtract(l.getLineDiscount()).max(BigDecimal.ZERO));
            l.setPromotionId(ld.getPromotionId());
            l.setPromotionName(ld.getReason());
        }
        for (com.silverline.erp.module.pos.dto.PromotionEval.FreeItem fi : promo.getFreeItems()) {
            BigDecimal value = fi.getUnitPrice().multiply(fi.getQty());
            Product fp = productService.findById(fi.getProductId());
            resp.getItems().add(new com.silverline.erp.module.pos.dto.CartPricing.PricedLine(
                    fi.getProductId(), fp != null ? fp.getName() : null, null, fi.getQty(),
                    fi.getUnitPrice(), value, BigDecimal.ZERO, true, fi.getPromotionId(), fi.getReason()));
        }
        resp.setPromotions(promo.getApplied());

        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = promo.getCartDiscount();
        for (com.silverline.erp.module.pos.dto.CartPricing.PricedLine l : resp.getItems()) {
            subTotal = subTotal.add(l.getUnitPrice().multiply(l.getQty()));
            discountTotal = discountTotal.add(l.getLineDiscount());
        }
        resp.setSubTotal(subTotal);
        resp.setDiscountTotal(discountTotal);
        resp.setNetTotal(subTotal.subtract(discountTotal).max(BigDecimal.ZERO));
        return resp;
    }

    private BigDecimal resolveUnitPrice(SaleItemRequest itemReq, Product product, Long branchId,
                                        com.silverline.erp.domain.inventory.Batch saleBatch) {
        String name = product != null && product.getName() != null ? product.getName().toLowerCase() : "";
        boolean isService = itemReq.getProductId() == 332L
                || name.contains("service") || name.contains("dialog tv") || name.contains("dtv");

        // Price precedence: the batch being sold (its own selling price) wins, then the
        // per-branch price (branch_product), then the global product catalog default.
        BigDecimal effectivePrice = resolveEffectiveSellingPrice(product, branchId, saleBatch);

        if (isService) {
            BigDecimal entered = itemReq.getUnitPrice();
            if (entered != null) return entered;
            return effectivePrice != null ? effectivePrice : BigDecimal.ZERO;
        }

        if (product == null || effectivePrice == null) {
            throw new com.silverline.erp.common.exception.ValidationException(
                    "Cannot sell product " + itemReq.getProductId() + ": no price configured for this branch.");
        }
        return effectivePrice;
    }

    /** Batch price if selling from a batch, else branch_product price, else the global default. */
    private BigDecimal resolveEffectiveSellingPrice(Product product, Long branchId,
                                                    com.silverline.erp.domain.inventory.Batch saleBatch) {
        if (product == null) return null;
        if (saleBatch != null && saleBatch.getSellingPrice() != null) {
            return saleBatch.getSellingPrice();
        }
        if (branchId != null) {
            BigDecimal branchPrice = branchProductRepository
                    .findByBranchIdAndProductId(branchId, product.getProductId())
                    .map(com.silverline.erp.domain.inventory.BranchProduct::getSellingPrice)
                    .orElse(null);
            if (branchPrice != null) return branchPrice;
        }
        return product.getSellingPrice();
    }

    /**
     * Coerces a client-requested status to an allowed value, defaulting to PAID.
     * Prevents a client from injecting an arbitrary status (mass-assignment).
     */
    private String normalizeStatus(String requested) {
        if (requested == null || requested.isBlank()) return "PAID";
        String upper = requested.toUpperCase();
        return CLIENT_ALLOWED_STATUSES.contains(upper) ? upper : "PAID";
    }

    /**
     * Clamps a discount into [0, max] so it can neither be negative nor exceed the value it applies to.
     * Prevents client-supplied discounts from producing negative totals (SEC-19).
     */
    private BigDecimal clampDiscount(BigDecimal discount, BigDecimal max) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (max != null && discount.compareTo(max) > 0) return max;
        return discount;
    }

    private String generateInvoiceNo() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String datePrefix = "INV-" + today;
        // Atomic per-day counter: concurrency-safe, no max()+1 race that could duplicate invoice numbers.
        int nextSequence = saleRepository.nextInvoiceSequence(datePrefix);
        return String.format("%s-%05d", datePrefix, nextSequence);
    }
}
