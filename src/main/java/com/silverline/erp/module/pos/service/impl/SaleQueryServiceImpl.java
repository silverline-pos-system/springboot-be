package com.silverline.erp.module.pos.service.impl;

import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.pos.dto.sale.PaymentResponse;
import com.silverline.erp.module.pos.dto.sale.ProductSalesHistoryDTO;
import com.silverline.erp.module.pos.dto.sale.SaleItemResponse;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.dto.sale.SaleSummaryDTO;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import com.silverline.erp.module.pos.repository.PaymentRepository;
import com.silverline.erp.module.pos.repository.SaleItemRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.module.pos.service.SaleQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SaleQueryServiceImpl implements SaleQueryService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Override
    public Map<String, Object> getLastInvoiceInfo() {
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

        String nextInvoiceNo = String.format("%s-%05d", datePrefix, nextSequence);

        Map<String, Object> result = new HashMap<>();
        result.put("lastInvoiceNo", lastInvoice);
        result.put("nextInvoiceNo", nextInvoiceNo);
        result.put("nextSequence", nextSequence);
        result.put("date", today);

        return result;
    }

    @Override
    public SaleResponse getSaleById(Long id) {
        return saleRepository.findById(id).map(sale -> {
            List<SaleItem> items = saleItemRepository.findBySaleId(id);
            List<Payment> payments = paymentRepository.findBySaleId(id);
            return mapToResponse(sale, items, payments);
        }).orElse(null);
    }

    @Override
    public List<SaleResponse> getSales(String status) {
        List<Sale> sales;
        if (status != null && !status.isEmpty()) {
            sales = saleRepository.findByPaymentStatus(status);
        } else {
            sales = saleRepository.findAll();
        }

        return sales.stream().map(sale -> {
            List<SaleItem> items = saleItemRepository.findBySaleId(sale.getSaleId());
            List<Payment> payments = paymentRepository.findBySaleId(sale.getSaleId());
            return mapToResponse(sale, items, payments);
        }).collect(Collectors.toList());
    }

    @Override
    public List<SaleResponse> getReturnableSales(Long branchId, Integer days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days != null ? days : 7);
        
        List<Sale> sales = saleRepository.findByDateRange(branchId, startDate, endDate);
        
        sales = sales.stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getPaymentStatus()))
                .collect(Collectors.toList());
        
        return sales.stream().map(sale -> {
            List<SaleItem> items = saleItemRepository.findBySaleId(sale.getSaleId());
            List<Payment> payments = paymentRepository.findBySaleId(sale.getSaleId());
            return mapToResponse(sale, items, payments);
        }).collect(Collectors.toList());
    }

    @Override
    public SaleResponse getSaleByInvoiceNo(String invoiceNo) {
        return saleRepository.findByInvoiceNo(invoiceNo).map(sale -> {
            List<SaleItem> items = saleItemRepository.findBySaleId(sale.getSaleId());
            List<Payment> payments = paymentRepository.findBySaleId(sale.getSaleId());
            return mapToResponse(sale, items, payments);
        }).orElse(null);
    }

    @Override
    public List<SaleSummaryDTO> getSaleSummaries(Long branchId, String status, String startDateStr, String endDateStr) {
        String dbStatus = status;
        if ("COMPLETED".equalsIgnoreCase(status)) {
            dbStatus = "PAID";
        } else if ("RECALL".equalsIgnoreCase(status)) {
            dbStatus = "HELD";
        }

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        try {
            if (startDateStr != null) startDate = LocalDateTime.parse(startDateStr.replace("Z", ""));
            if (endDateStr != null) endDate = LocalDateTime.parse(endDateStr.replace("Z", ""));
        } catch (Exception e) {
            log.warn("Date parsing error in getSaleSummaries: {}", e.getMessage());
        }

        List<Sale> sales;
        if (startDate != null && endDate != null) {
            sales = saleRepository.findByDateRange(branchId, startDate, endDate);
            if (dbStatus != null && !dbStatus.isEmpty()) {
                final String targetStatus = dbStatus;
                sales = sales.stream()
                        .filter(s -> targetStatus.equalsIgnoreCase(s.getPaymentStatus()))
                        .collect(Collectors.toList());
            }
        } else {
            if (dbStatus != null && !dbStatus.isEmpty()) {
                sales = saleRepository.findByPaymentStatus(dbStatus);
            } else {
                sales = saleRepository.findAll();
            }
            sales = sales.stream()
                    .filter(s -> s.getBranchId().equals(branchId))
                    .collect(Collectors.toList());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return sales.stream().map(sale -> {
            List<SaleItem> items = saleItemRepository.findBySaleId(sale.getSaleId());
            String customerName = "Walk-in Customer";
            if (sale.getCustomerId() != null) {
                customerName = customerRepository.findById(sale.getCustomerId())
                        .map(c -> c.getName()).orElse("Walk-in Customer");
            }

            return new SaleSummaryDTO(
                sale.getSaleId(),
                sale.getInvoiceNo(),
                sale.getSaleDate() != null ? sale.getSaleDate().format(formatter) : "",
                items.size(),
                sale.getNetTotal(),
                sale.getGrossTotal(),
                customerName,
                sale.getPaymentStatus()
            );
        }).collect(Collectors.toList());
    }

    @Override
    public List<SaleSummaryDTO> getHeldBills(Long branchId) {
        List<Sale> heldSales = saleRepository.findByPaymentStatus("HELD");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return heldSales.stream().map(sale -> {
            if (branchId != null && !branchId.equals(sale.getBranchId())) return null;
            
            List<SaleItem> items = saleItemRepository.findBySaleId(sale.getSaleId());
            String customerName = "Walk-in Customer";
            if (sale.getCustomerId() != null) {
                customerName = customerRepository.findById(sale.getCustomerId())
                        .map(c -> c.getName()).orElse("Walk-in Customer");
            }
            
            return new SaleSummaryDTO(
                sale.getSaleId(),
                sale.getInvoiceNo(),
                sale.getSaleDate() != null ? sale.getSaleDate().format(formatter) : "",
                items.size(),
                sale.getNetTotal(),
                sale.getGrossTotal(),
                customerName,
                sale.getPaymentStatus()
            );
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<ProductSalesHistoryDTO> getProductSalesHistory(Long productId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(java.time.LocalTime.MAX);

        List<ProductSalesHistoryDTO> results = saleItemRepository.findDailySalesByProduct(productId, start, end);
        if (results == null) return new ArrayList<>();
        return results;
    }

    @Override
    public SaleResponse mapToResponse(Sale sale, List<SaleItem> items, List<Payment> payments) {
        SaleResponse.Builder builder = new SaleResponse.Builder()
                .saleId(sale.getSaleId())
                .invoiceNo(sale.getInvoiceNo())
                .customerId(sale.getCustomerId())
                .grossTotal(sale.getGrossTotal())
                .discount(sale.getDiscount())
                .taxAmount(sale.getTaxAmount())
                .netTotal(sale.getNetTotal())
                .paidAmount(sale.getPaidAmount())
                .changeAmount(sale.getChangeAmount())
                .paymentStatus(sale.getPaymentStatus())
                .saleDate(sale.getSaleDate())
                .notes(sale.getNotes());

        if (sale.getCustomerId() != null) {
            customerRepository.findById(sale.getCustomerId())
                    .ifPresent(customer -> {
                        builder.customerName(customer.getName());
                        builder.customer(customer);
                    });
        }

        List<Long> productIds = items.stream().map(SaleItem::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        List<SaleItemResponse> itemResponses = items.stream().map(item -> {
            SaleItemResponse res = new SaleItemResponse();
            res.setSaleItemId(item.getSaleItemId());
            res.setProductId(item.getProductId());

            Product product = productMap.get(item.getProductId());
            if (product != null) {
                res.setProductName(product.getName());
            }

            res.setBatchId(item.getBatchId());
            res.setQuantity(item.getQty());
            res.setUnitPrice(item.getUnitPrice());
            res.setTotal(item.getTotal());
            return res;
        }).collect(Collectors.toList());
        builder.items(itemResponses);

        List<PaymentResponse> paymentResponses = payments.stream().map(p -> {
            PaymentResponse res = new PaymentResponse();
            res.setPaymentType(p.getPaymentType());
            res.setAmount(p.getAmount());
            return res;
        }).collect(Collectors.toList());
        builder.payments(paymentResponses);

        return builder.build();
    }
}
