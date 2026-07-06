package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.domain.pos.SalesReturn;
import com.silverline.erp.domain.pos.SalesReturnItem;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.UserProfileRepository;
import com.silverline.erp.module.inventory.service.StockService;
import com.silverline.erp.module.pos.dto.returns.ReturnRequest;
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

    @Override
    @Transactional
    public Long processReturn(ReturnRequest request) {
        log.info("Processing return for saleId={}, branchId={}, reason={}", request.getSaleId(), request.getBranchId(), request.getReason());
        String supUser = request.getSupervisorUsername();
        String supPass = request.getSupervisorPassword();

        if (supUser == null || supUser.isEmpty() || supPass == null || supPass.isEmpty()) {
            throw new RuntimeException("Supervisor approval is required for returns.");
        }

        Long supervisorId = null;
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(supUser, supPass)
            );
            if (auth.isAuthenticated()) {
                UserProfile supervisor = userProfileRepository.findByUsername(supUser)
                        .orElseThrow(() -> new RuntimeException("Supervisor not found"));
                supervisorId = supervisor.getUserId();
                String role = supervisor.getRole().name();
                if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role) && !"SUPERVISOR".equals(role)) {
                    throw new RuntimeException("User does not have supervisor privileges.");
                }
            } else {
                throw new RuntimeException("Invalid supervisor credentials");
            }
        } catch (Exception e) {
            log.warn("Business rule violation: supervisor authorization failed for user '{}': {}", supUser, e.getMessage());
            throw new RuntimeException("Supervisor authorization failed: " + e.getMessage());
        }

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
                SalesReturnItem item = new SalesReturnItem();
                item.setSalesReturn(ret);
                item.setSaleItemId(itemReq.getSaleItemId());
                item.setProductId(itemReq.getProductId());
                item.setQty(itemReq.getQty());
                item.setUnitPrice(itemReq.getUnitPrice());
                item.setTotal(itemReq.getUnitPrice().multiply(itemReq.getQty()));

                items.add(item);
                totalRefund = totalRefund.add(item.getTotal());

                try {
                    stockService.increaseStock(request.getBranchId(), itemReq.getProductId(), itemReq.getQty().intValue());
                } catch (Exception e) {
                    log.error("Failed to restock product {}: {}", itemReq.getProductId(), e.getMessage());
                }
            }
            salesReturnItemRepository.saveAll(items);
        }

        ret.setTotalAmount(totalRefund);
        salesReturnRepository.save(ret);

        String supervisorUsername = userProfileRepository.findById(supervisorId)
                .map(UserProfile::getUsername)
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
