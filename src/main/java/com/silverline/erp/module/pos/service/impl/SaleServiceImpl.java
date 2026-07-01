package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.common.audit.AuditLogService;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.system.SaasFeature;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.module.inventory.service.BatchService;
import com.silverline.erp.module.inventory.service.ProductService;
import com.silverline.erp.module.inventory.service.ProductSerialService;
import com.silverline.erp.module.inventory.service.StockService;
import com.silverline.erp.module.pos.dto.sale.CreateSaleRequest;
import com.silverline.erp.module.pos.dto.sale.PaymentRequest;
import com.silverline.erp.module.pos.dto.sale.SaleItemRequest;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.repository.PaymentRepository;
import com.silverline.erp.module.pos.repository.SaleItemRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.module.pos.service.SaleService;
import com.silverline.erp.module.pos.service.SaleQueryService;
import com.silverline.erp.module.admin.service.SaasFeatureService;
import com.silverline.erp.common.event.SaleCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service("posSaleService")
@Slf4j
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductService productService;
    private final StockService stockService;
    private final UserProfileRepo userProfileRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final BatchService batchService;
    private final SaasFeatureService featureService;
    private final ProductSerialService productSerialService;
    private final SaleQueryService saleQueryService;

    @Override
    @Transactional
    public SaleResponse createSale(CreateSaleRequest request, Long branchId, Long cashierId, Long shiftId) {
        String requestedStatus = (request.getStatus() != null && !request.getStatus().isEmpty())
                ? request.getStatus().toUpperCase() : "PAID";
        boolean isPaidSale = "PAID".equals(requestedStatus);

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

                    BigDecimal availableQty = BigDecimal.valueOf(stockService.getCurrentStock(branchId, itemReq.getProductId()));
                    
                    if (availableQty.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new RuntimeException("Cannot sell '" + productName + "' — item is out of stock (Available: 0)");
                    }
                    
                    if (availableQty.compareTo(requiredQty) < 0) {
                        throw new RuntimeException("Insufficient stock for '" + productName 
                                + "'. Requested: " + requiredQty + ", Available: " + availableQty);
                    }
                }
            } else {
                log.info("Stock validation bypassed for sale due to ALLOW_OUT_OF_STOCK feature being active.");
            }
        }

        Sale sale = null;
        boolean isUpdate = false;

        if (request.getSaleId() != null) {
            sale = saleRepository.findById(request.getSaleId()).orElse(null);
            if (sale != null && ("HELD".equalsIgnoreCase(sale.getPaymentStatus()) || "PENDING".equalsIgnoreCase(sale.getPaymentStatus()))) {
                isUpdate = true;
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

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            sale.setPaymentStatus(request.getStatus().toUpperCase());
        } else {
            sale.setPaymentStatus("PAID");
        }

        BigDecimal grossTotal = BigDecimal.ZERO;
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SaleItemRequest itemReq : request.getItems()) {
                BigDecimal unitPrice = itemReq.getUnitPrice();
                BigDecimal quantity = itemReq.getQuantity();

                if (unitPrice == null || quantity == null) {
                    Product product = productService.findById(itemReq.getProductId());
                    if (product != null) {
                        if (unitPrice == null) {
                            unitPrice = product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO;
                        }
                        if (quantity == null) {
                            quantity = BigDecimal.ONE;
                        }
                    } else {
                        unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
                        quantity = quantity != null ? quantity : BigDecimal.ONE;
                    }
                }

                BigDecimal lineTotal = unitPrice.multiply(quantity);
                BigDecimal itemDiscount = itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO;
                lineTotal = lineTotal.subtract(itemDiscount);
                grossTotal = grossTotal.add(lineTotal);
            }
        } else {
            if (request.getPayments() != null) {
                for (PaymentRequest pr : request.getPayments()) {
                    grossTotal = grossTotal.add(pr.getAmount() != null ? pr.getAmount() : BigDecimal.ZERO);
                }
            }
        }
        sale.setGrossTotal(grossTotal);

        BigDecimal netTotal = grossTotal.subtract(sale.getDiscount());
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
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SaleItemRequest itemReq : request.getItems()) {
                BigDecimal unitPrice = itemReq.getUnitPrice();
                BigDecimal quantity = itemReq.getQuantity();

                if (unitPrice == null) {
                    Product product = productService.findById(itemReq.getProductId());
                    unitPrice = (product != null && product.getSellingPrice() != null)
                            ? product.getSellingPrice() : BigDecimal.ZERO;
                }
                if (quantity == null) {
                    quantity = BigDecimal.ONE;
                }

                SaleItem item = new SaleItem();
                item.setSaleId(saleId);
                item.setProductId(itemReq.getProductId());
                item.setSerialId(itemReq.getSerialId());
                item.setBatchId(itemReq.getBatchId());
                item.setQty(quantity);
                item.setUnitPrice(unitPrice);
                item.setDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO);
                item.setTotal(unitPrice.multiply(quantity));

                saleItemRepository.save(item);
                saleItems.add(item);
            }
        }

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
                        
                        // Reduce stock via StockService
                        stockService.reduceStock(branchId, soldItem.getProductId(), soldQty.intValue());
                        log.info("Stock deducted for product {} in branch {}: -{}", soldItem.getProductId(), branchId, soldQty);

                        if (soldItem.getBatchId() != null) {
                            // Reduce batch stock via BatchService
                            batchService.deductBatchStock(soldItem.getBatchId(), soldQty);
                            log.info("Batch {} deducted by {}", soldItem.getBatchId(), soldQty);
                        }

                        if (soldItem.getSerialId() != null) {
                            // Mark serial as sold via ProductSerialService
                            productSerialService.markAsSold(soldItem.getSerialId(), finalSale.getSaleId());
                            log.info("Serial {} marked as SOLD for Sale {}", soldItem.getSerialId(), finalSale.getInvoiceNo());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to deduct stock for product {}: {}", soldItem.getProductId(), e.getMessage());
                }
            }
        }

        String cashierUsername = userProfileRepo.findById(cashierId)
                .map(UserProfile::getUsername)
                .orElse("Cashier #" + cashierId);

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

    private String generateInvoiceNo() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String datePrefix = "INV-" + today;

        String lastInvoice = saleRepository.findLastInvoiceNoByDatePrefix(datePrefix);

        int nextSequence = 1;
        if (lastInvoice != null && lastInvoice.startsWith(datePrefix)) {
            try {
                String[] parts = lastInvoice.split("-");
                if (parts.length >= 3) {
                    nextSequence = Integer.parseInt(parts[2]) + 1;
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }

        return String.format("%s-%05d", datePrefix, nextSequence);
    }
}
