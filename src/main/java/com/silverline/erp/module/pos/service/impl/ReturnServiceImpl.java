package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.common.exception.ValidationException;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.domain.pos.SalesReturn;
import com.silverline.erp.domain.pos.SalesReturnItem;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.inventory.service.ProductSerialService;
import com.silverline.erp.module.inventory.service.StockService;
import com.silverline.erp.module.pos.dto.returns.ReturnRequest;
import com.silverline.erp.module.pos.repository.SaleItemRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.module.pos.repository.SalesReturnItemRepository;
import com.silverline.erp.module.pos.repository.SalesReturnRepository;
import com.silverline.erp.module.pos.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthenticationManager authenticationManager;
    private final StockService stockService;
    private final AuditLogService activityLogService;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductSerialService productSerialService;

    @Override
    @Transactional
    public Long processReturn(ReturnRequest request) {
        log.info("Processing return for saleId={}, branchId={}, reason={}", request.getSaleId(), request.getBranchId(), request.getReason());
        String supUser = request.getSupervisorUsername();
        String supPass = request.getSupervisorPassword();

        if (supUser == null || supUser.isEmpty() || supPass == null || supPass.isEmpty()) {
            throw new com.silverline.erp.common.exception.UnauthorizedException("Supervisor approval is required for returns.");
        }

        Long supervisorId = null;
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(supUser, supPass)
            );
            if (auth.isAuthenticated()) {
                UserProfile supervisor = userProfileRepository.findByUsername(supUser)
                        .orElseThrow(() -> new com.silverline.erp.common.exception.ResourceNotFoundException("Supervisor not found"));
                supervisorId = supervisor.getUserId();
                String role = supervisor.getRole().name();
                if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role) && !"SUPERVISOR".equals(role)) {
                    throw new com.silverline.erp.common.exception.UnauthorizedException("User does not have supervisor privileges.");
                }
            } else {
                throw new com.silverline.erp.common.exception.UnauthorizedException("Invalid supervisor credentials");
            }
        } catch (Exception e) {
            log.warn("Business rule violation: supervisor authorization failed for user '{}': {}", supUser, e.getMessage());
            if (e instanceof com.silverline.erp.common.exception.BusinessException) {
                throw e;
            }
            throw new com.silverline.erp.common.exception.UnauthorizedException("Supervisor authorization failed: " + e.getMessage());
        }

        // --- Validate the return against the original sale (SEC-15/16, DI-06/07/08) ---
        // Without this, any product/qty/price could be refunded against any sale id, or the same
        // item refunded repeatedly. The original sale is the source of truth for what may be refunded.
        Sale originalSale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ValidationException("Return rejected: original sale " + request.getSaleId() + " not found."));

        SalesReturn ret = new SalesReturn();
        ret.setReturnNo(generateReturnNo());
        ret.setSaleId(request.getSaleId());
        ret.setBranchId(request.getBranchId());
        ret.setReturnDate(LocalDateTime.now());
        ret.setReason(request.getReason());
        ret.setRefundMethod(request.getRefundMethod());
        ret.setStatus("APPROVED");

        ret = salesReturnRepository.save(ret);

        BigDecimal totalRefund = BigDecimal.ZERO;
        List<SalesReturnItem> items = new ArrayList<>();

        if (request.getItems() != null) {
            for (ReturnRequest.ReturnItemRequest itemReq : request.getItems()) {
                // 1. The referenced sale item must exist and belong to THIS sale.
                SaleItem originalItem = saleItemRepository.findById(itemReq.getSaleItemId())
                        .orElseThrow(() -> new ValidationException("Return rejected: sale item " + itemReq.getSaleItemId() + " not found."));
                if (!originalItem.getSaleId().equals(originalSale.getSaleId())) {
                    throw new ValidationException("Return rejected: sale item " + itemReq.getSaleItemId()
                            + " does not belong to sale " + originalSale.getSaleId() + ".");
                }
                // 2. The product must match the original line (no swapping to a pricier product).
                if (!originalItem.getProductId().equals(itemReq.getProductId())) {
                    throw new ValidationException("Return rejected: product mismatch for sale item " + itemReq.getSaleItemId() + ".");
                }
                // 3. Cannot return more than was sold minus what was already returned.
                BigDecimal alreadyReturned = salesReturnItemRepository.sumReturnedQtyBySaleItemId(itemReq.getSaleItemId());
                if (alreadyReturned == null) alreadyReturned = BigDecimal.ZERO;
                BigDecimal remaining = originalItem.getQty().subtract(alreadyReturned);
                if (itemReq.getQty().compareTo(remaining) > 0) {
                    throw new ValidationException("Return rejected: requested " + itemReq.getQty()
                            + " for sale item " + itemReq.getSaleItemId() + " but only " + remaining + " remain returnable.");
                }

                // 4. Refund price comes from the original sale line, never from the client request.
                BigDecimal refundUnitPrice = originalItem.getUnitPrice() != null ? originalItem.getUnitPrice() : BigDecimal.ZERO;

                SalesReturnItem item = new SalesReturnItem();
                item.setSalesReturn(ret);
                item.setSaleItemId(itemReq.getSaleItemId());
                item.setProductId(originalItem.getProductId());
                item.setQty(itemReq.getQty());
                item.setUnitPrice(refundUnitPrice);
                item.setTotal(refundUnitPrice.multiply(itemReq.getQty()));

                items.add(item);
                totalRefund = totalRefund.add(item.getTotal());

                try {
                    if (originalItem.getSerialId() != null) {
                        // Serialized item: restore that specific serial. markAsReturned sets the serial to
                        // RETURNED and increments stock by 1, so do NOT also call increaseStock (double count).
                        productSerialService.markAsReturned(originalItem.getSerialId());
                    } else {
                        // Non-serialized: restock the exact (possibly fractional) quantity.
                        stockService.increaseStock(request.getBranchId(), originalItem.getProductId(), itemReq.getQty());
                    }
                } catch (Exception e) {
                    // Re-throw so the whole refund rolls back rather than paying the customer without
                    // returning stock (DI-09/10).
                    log.error("Restock failed for product {} - rolling back return", originalItem.getProductId(), e);
                    throw new ValidationException("Return cancelled: could not restock product " + originalItem.getProductId() + ". " + e.getMessage());
                }
            }
            salesReturnItemRepository.saveAll(items);
        }

        ret.setTotalAmount(totalRefund);
        salesReturnRepository.save(ret);

        String supervisorUsername = userProfileRepository.findById(supervisorId)
                .map(u -> u.getUsername())
                .orElse("Supervisor #" + supervisorId);

        activityLogService.logActivity(
                request.getBranchId(),
                null,
                supervisorId,
                supervisorUsername,
                "SUPERVISOR",
                "RETURN",
                "RETURN",
                ret.getReturnId(),
                "Return processed for Sale #" + request.getSaleId() + ". Refund: " + totalRefund,
                "{\"itemCount\":" + items.size() + "}"
        );

        return ret.getReturnId();
    }

    private String generateReturnNo() {
        return "RET-" + System.currentTimeMillis();
    }
}
