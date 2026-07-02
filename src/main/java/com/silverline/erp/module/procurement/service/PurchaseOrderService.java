package com.silverline.erp.module.procurement.service;

import com.silverline.erp.domain.inventory.PurchaseOrder;
import com.silverline.erp.domain.inventory.PurchaseOrderItem;
import com.silverline.erp.domain.inventory.PurchaseOrderPayment;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.inventory.dto.ProcessPORequest;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.SupplierRepository;
import com.silverline.erp.module.procurement.dto.*;
import com.silverline.erp.module.procurement.repository.PurchaseOrderItemRepository;
import com.silverline.erp.module.procurement.repository.PurchaseOrderPaymentRepository;
import com.silverline.erp.module.procurement.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;

    private Pageable capPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int cappedSize = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), cappedSize, pageable.getSort());
    }

    private final PurchaseOrderItemRepository poItemRepository;

    private final PurchaseOrderPaymentRepository poPaymentRepository;

    private final SupplierRepository supplierRepository;

    private final ProductRepository productRepository;

    private final UserRepository userRepository;

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderDTO dto) {
        PurchaseOrder po = new PurchaseOrder();
        
        // Generate a random PO No if not provided, or a formatted one.
        po.setPoNo(dto.getPoNo() != null && !dto.getPoNo().isEmpty() ? dto.getPoNo() : "PO-" + System.currentTimeMillis());
        
        po.setBranchId(dto.getBranchId() != null ? dto.getBranchId() : 1L);
        po.setSupplierId(dto.getSupplierId());
        po.setPoDate(dto.getPoDate() != null ? dto.getPoDate() : LocalDate.now());
        po.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        po.setPaymentTerms(dto.getPaymentTerms());
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;

        po.setStatus("PENDING_APPROVAL"); // Default to pending approval mode
        po.setCreatedBy(resolveCreatorId(dto.getCreatedBy()));

        po = poRepository.save(po);

        for (PurchaseOrderItemDTO itemDto : dto.getItems()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPoId(po.getPoId());
            item.setProductId(itemDto.getProductId());
            item.setQtyOrdered(itemDto.getQtyOrdered());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setSellingPrice(itemDto.getSellingPrice());
            item.setMrp(itemDto.getMrp());
            item.setDiscount(itemDto.getDiscount() != null ? itemDto.getDiscount() : BigDecimal.ZERO);
            
            // Calc Total
            BigDecimal lineTotal = item.getQtyOrdered().multiply(item.getUnitPrice());
            BigDecimal itemTotal = lineTotal.subtract(item.getDiscount());
            item.setTotal(itemTotal);

            poItemRepository.save(item);

            totalAmount = totalAmount.add(lineTotal);
            discountAmount = discountAmount.add(item.getDiscount());
            netAmount = netAmount.add(itemTotal);
        }

        po.setTotalAmount(totalAmount);
        po.setDiscountAmount(discountAmount);
        po.setNetAmount(netAmount);
        return poRepository.save(po);
    }

    public Page<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable) {
        Pageable capped = capPageable(pageable);
        List<PurchaseOrderResponse> all = poRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        int start = (int) capped.getOffset();
        int end = Math.min((start + capped.getPageSize()), all.size());
        List<PurchaseOrderResponse> content = start < all.size() ? all.subList(start, end) : List.of();
        return new PageImpl<>(content, capped, all.size());
    }
    
    public List<PurchaseOrderResponse> getManagerApprovals() {
        return poRepository.findByStatus("PENDING_APPROVAL").stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PurchaseOrderResponse> getPurchaseOrdersByStatus(String status) {
        return poRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PurchaseOrder processPO(Long poId, ProcessPORequest request) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        String status = request.getStatus();
        po.setStatus(status);

        if ("PAID".equals(status)) {
            // Transaction rule: Insert payment + update PO
            PurchaseOrderPayment payment = new PurchaseOrderPayment();
            payment.setPoId(po.getPoId());
            payment.setPaymentMethod(request.getPaymentMethod());
            payment.setPaymentReference(request.getPaymentReference());
            payment.setAmountPaid(request.getAmountPaid() != null ? request.getAmountPaid() : po.getNetAmount());
            payment.setPaidAt(request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now());
            payment.setNotes(request.getNotes());

            poPaymentRepository.save(payment);

            po.setPaymentStatus("PAID");
            po.setPaidAmount(payment.getAmountPaid());
            po.setPaidAt(payment.getPaidAt());
        } else if ("APPROVED".equals(status) || "REJECTED".equals(status) 
                || "TRANSFERRED_TO_CASHIER".equals(status)
                || "PARTIALLY_RECEIVED".equals(status)
                || "FULLY_RECEIVED".equals(status)) {
            // Status update only
        }

        return poRepository.save(po);
    }

    public List<PurchaseOrderItemResponse> getPurchaseOrderItems(Long poId) {
        return poItemRepository.findByPoId(poId).stream()
            .map(item -> {
                PurchaseOrderItemResponse res = new PurchaseOrderItemResponse();
                res.setPoItemId(item.getPoItemId());
                res.setPoId(item.getPoId());
                res.setProductId(item.getProductId());

                // Fetch product name
                String productName = productRepository.findById(item.getProductId())
                        .map(p -> p.getName())
                        .orElse("Unknown Product");
                res.setProductName(productName);

                res.setQtyOrdered(item.getQtyOrdered());
                res.setQtyDispatched(item.getQtyDispatched());
                res.setUnitPrice(item.getUnitPrice());
                res.setSellingPrice(item.getSellingPrice());
                res.setMrp(item.getMrp());
                res.setDiscount(item.getDiscount());
                res.setTotal(item.getTotal());
                return res;
            })
            .collect(Collectors.toList());
    }

    public List<PurchaseOrderPaymentResponse> getPurchaseOrderPayments(Long poId) {
        return poPaymentRepository.findByPoIdOrderByPaidAtDesc(poId).stream()
            .map(payment -> {
                PurchaseOrderPaymentResponse res = new PurchaseOrderPaymentResponse();
                res.setPoPaymentId(payment.getPoPaymentId());
                res.setPoId(payment.getPoId());
                res.setPaymentMethod(payment.getPaymentMethod());
                res.setPaymentReference(payment.getPaymentReference());
                res.setAmountPaid(payment.getAmountPaid());
                res.setPaidAt(payment.getPaidAt());
                res.setPaidBy(payment.getPaidBy());
                res.setNotes(payment.getNotes());
                return res;
            })
            .collect(Collectors.toList());
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        PurchaseOrderResponse response = new PurchaseOrderResponse();
        response.setPoId(po.getPoId());
        response.setPoNo(po.getPoNo());
        response.setBranchId(po.getBranchId());
        response.setSupplierId(po.getSupplierId());

        // Fetch supplier name
        String supplierName = supplierRepository.findById(po.getSupplierId())
                .map(s -> s.getName())
                .orElse("Unknown Supplier");
        response.setSupplierName(supplierName);

        response.setPoDate(po.getPoDate());
        response.setExpectedDeliveryDate(po.getExpectedDeliveryDate());
        response.setPaymentTerms(po.getPaymentTerms());
        response.setTotalAmount(po.getTotalAmount());
        response.setDiscountAmount(po.getDiscountAmount());
        response.setNetAmount(po.getNetAmount());
        response.setStatus(po.getStatus());
        response.setCreatedBy(po.getCreatedBy());
        response.setRequestedBy(po.getCreatedBy() != null ?
            userRepository.findById(po.getCreatedBy())
                .map(u -> (u.getUsername() != null && !u.getUsername().trim().isEmpty()) ? u.getUsername() : u.getFullName())
                .orElse("ID: " + po.getCreatedBy()) : "System");
        response.setCreatedAt(po.getCreatedAt());

        // Payment fields
        response.setPaymentStatus(po.getPaymentStatus());
        response.setPaidAmount(po.getPaidAmount());
        response.setPaidDate(po.getPaidAt());

        // If there's a recent payment, we might want to show method/ref,
        // but the requirement says "response item fields must include payment fields".
        // The example shows "paymentMethod": "BANK_TRANSFER", etc.
        // This implies the LATEST payment or the one making it PAID.
        if ("PAID".equals(po.getPaymentStatus()) || po.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            List<PurchaseOrderPayment> payments = poPaymentRepository.findByPoIdOrderByPaidAtDesc(po.getPoId());
            if (!payments.isEmpty()) {
                PurchaseOrderPayment latest = payments.get(0);
                response.setPaymentMethod(latest.getPaymentMethod());
                response.setPaymentReference(latest.getPaymentReference());
            }
        }

        return response;
    }

    private Long resolveCreatorId(Long requestCreatedBy) {
        if (requestCreatedBy != null) {
            return requestCreatedBy;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        if (username == null || username.trim().isEmpty() || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }

        return userRepository.findByUsername(username)
                .map(u -> u.getUserId())
                .orElse(null);
    }
}

