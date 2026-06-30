package com.silverline.erp.module.pos.service;

import com.silverline.erp.common.email.EmailService;
import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.module.inventory.repository.BatchRepository;
import com.silverline.erp.module.pos.dto.customer.CreateCustomerRequest;
import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.pos.SaleItem;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.StockRepository;
import com.silverline.erp.domain.inventory.Stock;
import lombok.extern.slf4j.Slf4j;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.module.pos.dto.sale.CreateSaleRequest;
import com.silverline.erp.module.pos.dto.sale.PaymentRequest;
import com.silverline.erp.module.pos.dto.sale.SaleItemRequest;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.dto.sale.SaleItemResponse;
import com.silverline.erp.module.pos.dto.sale.PaymentResponse;
import com.silverline.erp.module.pos.dto.sale.SaleSummaryDTO;
import com.silverline.erp.module.pos.dto.sale.ProductSalesHistoryDTO;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import com.silverline.erp.module.pos.repository.PaymentRepository;
import com.silverline.erp.module.pos.repository.SaleItemRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.domain.pos.SalesReturn;
import com.silverline.erp.domain.pos.SalesReturnItem;
import com.silverline.erp.module.pos.repository.SalesReturnRepository;
import com.silverline.erp.module.pos.repository.SalesReturnItemRepository;
import com.silverline.erp.module.pos.dto.returns.ReturnRequest;
import com.silverline.erp.module.inventory.repository.ProductSerialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.domain.enums.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.silverline.erp.common.audit.AuditLogService;
import org.springframework.security.core.Authentication;
import com.silverline.erp.module.admin.repository.SaasFeatureRepository;
import com.silverline.erp.domain.system.SaasFeature;

@Service
@Slf4j
public class PosService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final UserProfileRepo userProfileRepo;
    private final AuthenticationManager authenticationManager;
    private final StockRepository stockRepository;
    private final AuditLogService activityLogService;
    private final com.silverline.erp.module.inventory.repository.BatchRepository batchRepository;
    private final com.silverline.erp.common.email.EmailService emailService;
    private final SaasFeatureRepository featureRepository;
    private final ProductSerialRepository productSerialRepository;
    
    // In-memory OTP store for simplicity (customerId -> OTP)
    private final Map<Long, String> loyaltyOtpStore = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    public PosService(SaleRepository saleRepository,
                      SaleItemRepository saleItemRepository,
                      PaymentRepository paymentRepository,
                      CustomerRepository customerRepository,
                      ProductRepository productRepository,
                      SalesReturnRepository salesReturnRepository,
                      SalesReturnItemRepository salesReturnItemRepository,
                      UserProfileRepo userProfileRepo,
                      AuthenticationManager authenticationManager,
                      StockRepository stockRepository,
                      AuditLogService activityLogService,
                      com.silverline.erp.module.inventory.repository.BatchRepository batchRepository,
                      com.silverline.erp.common.email.EmailService emailService,
                      SaasFeatureRepository featureRepository,
                      ProductSerialRepository productSerialRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.salesReturnRepository = salesReturnRepository;
        this.salesReturnItemRepository = salesReturnItemRepository;
        this.userProfileRepo = userProfileRepo;
        this.authenticationManager = authenticationManager;
        this.stockRepository = stockRepository;
        this.activityLogService = activityLogService;
        this.batchRepository = batchRepository;
        this.emailService = emailService;
        this.featureRepository = featureRepository;
        this.productSerialRepository = productSerialRepository;
    }

    @Transactional
    public SaleResponse createSale(CreateSaleRequest request, Long branchId, Long cashierId, Long shiftId) {
        // Determine if this is a PAID sale (not just HELD)
        String requestedStatus = (request.getStatus() != null && !request.getStatus().isEmpty())
                ? request.getStatus().toUpperCase() : "PAID";
        boolean isPaidSale = "PAID".equals(requestedStatus);

        // --- STOCK VALIDATION: Prohibit zero-quantity item selling UNLESS ALLOW_OUT_OF_STOCK is enabled ---
        if (isPaidSale && request.getItems() != null && !request.getItems().isEmpty()) {
            boolean allowOutOfStock = featureRepository.findByFeatureCode("ALLOW_OUT_OF_STOCK")
                    .map(SaasFeature::getIsActive)
                    .orElse(false);

            if (!allowOutOfStock) {
                for (SaleItemRequest itemReq : request.getItems()) {
                    BigDecimal requiredQty = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
                    
                    Product product = productRepository.findById(itemReq.getProductId()).orElse(null);
                    String productName = product != null ? product.getName() : "Product #" + itemReq.getProductId();
                    
                    // Bypass stock check for services
                    if (itemReq.getProductId() == 332L || productName.toLowerCase().contains("service") || productName.toLowerCase().contains("dialog tv") || productName.toLowerCase().contains("dtv")) {
                        continue;
                    }

                    // Get available stock for this product in this branch
                    java.util.Optional<Stock> stockOpt = stockRepository.findByBranchIdAndProductId(branchId, itemReq.getProductId());
                    BigDecimal availableQty = stockOpt.map(Stock::getAvailableQty).orElse(BigDecimal.ZERO);
                    
                    if (availableQty.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new RuntimeException("Cannot sell '" + productName + "' â€” item is out of stock (Available: 0)");
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

        // 1. Create OR Update Sale Entity
        Sale sale = null;
        boolean isUpdate = false;

        if (request.getSaleId() != null) {
            sale = saleRepository.findById(request.getSaleId()).orElse(null);
            // Only allow updating if it was HELD or PENDING
            if (sale != null && ("HELD".equalsIgnoreCase(sale.getPaymentStatus()) || "PENDING".equalsIgnoreCase(sale.getPaymentStatus()))) {
                isUpdate = true;
                // Clear existing items/payments to replace them (simplest approach for sync)
                saleItemRepository.deleteBySaleId(sale.getSaleId());
                paymentRepository.deleteBySaleId(sale.getSaleId());
            } else {
                // Invalid ID or status, fallback to create new (or could throw error)
                sale = new Sale();
                sale.setInvoiceNo(generateInvoiceNo());
                sale.setSaleDate(LocalDateTime.now()); // Set date for new
            }
        } else {
            sale = new Sale();
            sale.setInvoiceNo(generateInvoiceNo());
            sale.setSaleDate(LocalDateTime.now());
        }

        sale.setBranchId(branchId);
        sale.setCashierId(cashierId);
        sale.setCustomerId(request.getCustomerId());
        if (!isUpdate) { // Only change shift if it's new, otherwise keep original shift? Or update to current?
             // Usually if you recall and pay, it belongs to the *current* shift.
             sale.setShiftId(shiftId);
        } else {
             sale.setShiftId(shiftId); // Update shift to current active shift on payment
        }
        
        // If updating, date might need to be refreshed or kept?
        // Let's refresh date to "Now" if we are paying. If just holding again, maybe keep or update.
        // Let's update it to allow accurate reporting.
        sale.setSaleDate(LocalDateTime.now());

        sale.setNotes(request.getNotes());
        sale.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        sale.setTaxAmount(BigDecimal.ZERO); // Calculate taxes if needed
        sale.setSaleType(request.getSaleType() != null ? request.getSaleType() : "RETAIL");

        // Determine status
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            sale.setPaymentStatus(request.getStatus().toUpperCase());
        } else {
             sale.setPaymentStatus("PAID"); // Default
        }

        // Calculate totals
        BigDecimal grossTotal = BigDecimal.ZERO;
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SaleItemRequest itemReq : request.getItems()) {
                BigDecimal unitPrice = itemReq.getUnitPrice();
                BigDecimal quantity = itemReq.getQuantity();

                // Handle null values - fetch from product if needed
                if (unitPrice == null || quantity == null) {
                    Product product = productRepository.findById(itemReq.getProductId()).orElse(null);
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
            // Service-only sale: use payment total as gross
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
            for(PaymentRequest pr : request.getPayments()) {
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

        // Save Sale
        Long saleId = saleRepository.save(sale);
        sale.setSaleId(saleId);

        // 2. Save Items
        List<SaleItem> saleItems = new ArrayList<>();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SaleItemRequest itemReq : request.getItems()) {
                BigDecimal unitPrice = itemReq.getUnitPrice();
                BigDecimal quantity = itemReq.getQuantity();

                // Handle null values - fetch from product if needed
                if (unitPrice == null) {
                    Product product = productRepository.findById(itemReq.getProductId()).orElse(null);
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

        // 3. Save Payments
        List<Payment> payments = new ArrayList<>();
        if (request.getPayments() != null) {
            for(PaymentRequest pr : request.getPayments()) {
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

        // --- STOCK DEDUCTION: Deduct stock for PAID sales ---
        final Sale finalSale = sale;
        if ("PAID".equals(sale.getPaymentStatus()) && !saleItems.isEmpty()) {
            for (SaleItem soldItem : saleItems) {
                try {
                    Product product = productRepository.findById(soldItem.getProductId()).orElse(null);
                    String productName = product != null ? product.getName() : "";
                    
                    if (!(soldItem.getProductId() == 332L || productName.toLowerCase().contains("service") || productName.toLowerCase().contains("dialog tv") || productName.toLowerCase().contains("dtv"))) {
                        java.util.Optional<Stock> stockOpt = stockRepository.findByBranchIdAndProductId(branchId, soldItem.getProductId());
                        if (stockOpt.isPresent()) {
                            Stock stock = stockOpt.get();
                            BigDecimal soldQty = soldItem.getQty() != null ? soldItem.getQty() : BigDecimal.ONE;
                            stock.setQuantity(stock.getQuantity().subtract(soldQty));
                            stock.setAvailableQty(stock.getAvailableQty().subtract(soldQty));
                            stockRepository.save(stock);
                            log.info("Stock deducted for product {} in branch {}: -{}", soldItem.getProductId(), branchId, soldQty);

                            if (soldItem.getBatchId() != null) {
                                java.util.Optional<com.silverline.erp.domain.inventory.Batch> batchOpt = batchRepository.findById(soldItem.getBatchId());
                                if (batchOpt.isPresent()) {
                                    com.silverline.erp.domain.inventory.Batch b = batchOpt.get();
                                    b.setQty(b.getQty().subtract(soldQty));
                                    batchRepository.save(b);
                                    log.info("Batch {} deducted by {}", b.getBatchId(), soldQty);
                                }
                            }

                            // Update Serial Status if applicable
                            if (soldItem.getSerialId() != null) {
                                productSerialRepository.findById(soldItem.getSerialId()).ifPresent(serial -> {
                                    serial.setStatus("SOLD");
                                    serial.setSaleId(finalSale.getSaleId());
                                    serial.setSoldAt(LocalDateTime.now());
                                    productSerialRepository.save(serial);
                                    log.info("Serial {} marked as SOLD for Sale {}", serial.getSerialNo(), finalSale.getInvoiceNo());
                                });
                            }
                        } else {
                            log.warn("No stock record found for product {} in branch {} during sale deduction", soldItem.getProductId(), branchId);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to deduct stock for product {}: {}", soldItem.getProductId(), e.getMessage());
                }
            }
        }

        // Fetch username for logging
        String cashierUsername = userProfileRepo.findById(cashierId)
                .map(UserProfile::getUsername)
                .orElse("Cashier #" + cashierId);

        // Log Activity
        activityLogService.logActivity(
            branchId,
            null, // Could fetch terminal ID if available, but might not be passed here
            cashierId,
            cashierUsername, 
            "CASHIER",
            "SALE",
            "ORDER",
            sale.getSaleId(), // Use ID instead of Invoice String for BIGINT compatibility
            "Retail Sale " + sale.getInvoiceNo() + ". Total: " + sale.getNetTotal(),
            "{\"itemCount\":" + saleItems.size() + "}"
        );

        return mapToResponse(sale, saleItems, payments);
    }

    /**
     * Get the last invoice number for today
     * @return Last invoice info with number and next sequence
     */
    public Map<String, Object> getLastInvoiceInfo() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String datePrefix = "INV-" + today;

        String lastInvoice = saleRepository.findLastInvoiceNoByDatePrefix(datePrefix);

        int nextSequence = 1;
        if (lastInvoice != null && lastInvoice.startsWith(datePrefix)) {
            try {
                // Extract the sequence number from INV-YYYYMMDD-XXXXX
                String[] parts = lastInvoice.split("-");
                if (parts.length >= 3) {
                    nextSequence = Integer.parseInt(parts[2]) + 1;
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }

        String nextInvoiceNo = String.format("%s-%05d", datePrefix, nextSequence);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("lastInvoiceNo", lastInvoice);
        result.put("nextInvoiceNo", nextInvoiceNo);
        result.put("nextSequence", nextSequence);
        result.put("date", today);

        return result;
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

    private SaleResponse mapToResponse(Sale sale, List<SaleItem> items, List<Payment> payments) {
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

        // Fetch Customer Name and full customer object
        if (sale.getCustomerId() != null) {
            customerRepository.findById(sale.getCustomerId())
                    .ifPresent(customer -> {
                        builder.customerName(customer.getName());
                        builder.customer(customer); // Pass full Customer entity
                    });
        }

        // Fetch Product Names optimization
        List<Long> productIds = items.stream().map(SaleItem::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        List<SaleItemResponse> itemResponses = items.stream().map(item -> {
            SaleItemResponse res = new SaleItemResponse();
            res.setSaleItemId(item.getSaleItemId()); // Ensure ID is set
            res.setProductId(item.getProductId());

            Product product = productMap.get(item.getProductId());
            if (product != null) {
                res.setProductName(product.getName());
                // res.setSku(product.getSku());
                // res.setBarcode(product.getBarcode());
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

    public SaleResponse getSaleById(Long id) {
       // Implementation for getBillById
        return saleRepository.findById(id).map(sale -> {
            List<SaleItem> items = saleItemRepository.findBySaleId(id);
            List<Payment> payments = paymentRepository.findBySaleId(id);
            return mapToResponse(sale, items, payments);
        }).orElse(null);
    }

    /**
     * Get returnable sales - PAID sales from last N days with full item details
     */
    public List<SaleResponse> getReturnableSales(Long branchId, Integer days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days != null ? days : 7);
        
        List<Sale> sales = saleRepository.findByDateRange(branchId, startDate, endDate);
        
        // Filter to only PAID (completed) sales
        sales = sales.stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getPaymentStatus()))
                .collect(Collectors.toList());
        
        return sales.stream().map(sale -> {
            List<SaleItem> items = saleItemRepository.findBySaleId(sale.getSaleId());
            List<Payment> payments = paymentRepository.findBySaleId(sale.getSaleId());
            return mapToResponse(sale, items, payments);
        }).collect(Collectors.toList());
    }

    /**
     * Get sale by invoice number for return lookup
     */
    public SaleResponse getSaleByInvoiceNo(String invoiceNo) {
        return saleRepository.findByInvoiceNo(invoiceNo).map(sale -> {
            List<SaleItem> items = saleItemRepository.findBySaleId(sale.getSaleId());
            List<Payment> payments = paymentRepository.findBySaleId(sale.getSaleId());
            return mapToResponse(sale, items, payments);
        }).orElse(null);
    }

    public List<SaleSummaryDTO> getSaleSummaries(Long branchId, String status, String startDateStr, String endDateStr) {
        // Map frontend status to backend status
        String dbStatus = status;
        if ("COMPLETED".equalsIgnoreCase(status)) {
            dbStatus = "PAID";
        } else if ("RECALL".equalsIgnoreCase(status)) {
            dbStatus = "HELD";
        }

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        try {
             if (startDateStr != null) startDate = LocalDateTime.parse(startDateStr.replace("Z", "")); // Simple ISO fix or use formatter
             if (endDateStr != null) endDate = LocalDateTime.parse(endDateStr.replace("Z", ""));
        } catch (Exception e) {
            // If parsing fails, ignore dates or set defaults
             System.err.println("Date parsing error: " + e.getMessage());
        }

        List<Sale> sales;
        if (startDate != null && endDate != null) {
            sales = saleRepository.findByDateRange(branchId, startDate, endDate);
            // Filter by status if provided
            if (dbStatus != null && !dbStatus.isEmpty()) {
                 final String targetStatus = dbStatus;
                 sales = sales.stream()
                        .filter(s -> targetStatus.equalsIgnoreCase(s.getPaymentStatus()))
                        .collect(Collectors.toList());
            }
        } else {
             // Fallback to all (restricted by status if present)
             // WARN: This might fetch other branches if repository method doesn't filter!
             // Ideally we should have findByBranchAndStatus
             if (dbStatus != null && !dbStatus.isEmpty()) {
                  sales = saleRepository.findByPaymentStatus(dbStatus);
             } else {
                 sales = saleRepository.findAll();
             }
             // Filter by branch manually to be safe
             sales = sales.stream()
                     .filter(s -> s.getBranchId().equals(branchId))
                     .collect(Collectors.toList());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return sales.stream().map(sale -> {
            // Optimization: count items instead of fetching all if repository supports it
            // For now, fetch list to get count
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

    public com.silverline.erp.domain.pos.Customer createCustomer(com.silverline.erp.module.pos.dto.customer.CreateCustomerRequest request) {
        // Validate
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new RuntimeException("Customer Name is required");
        }
        if (request.getPhone() == null || request.getPhone().isEmpty()) {
            throw new RuntimeException("Phone is required");
        }
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        // Check duplicate
        if (customerRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new RuntimeException("Customer with this phone already exists");
        }

        com.silverline.erp.domain.pos.Customer customer = new com.silverline.erp.domain.pos.Customer();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setLoyaltyPoints(0);
        customer.setTotalPurchases(BigDecimal.ZERO);
        customer.setIsActive(true);
        customer.setCode(generateCustomerCode(request.getPhone()));

        return customerRepository.save(customer);
    }

    public void updateLoyaltyPoints(Long customerId, Integer points) {
        customerRepository.findById(customerId).ifPresent(customer -> {
            Integer current = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
            customer.setLoyaltyPoints(current + points);
            customerRepository.save(customer);
            
            // Log Activity
            try {
                activityLogService.logActivity(
                    1L, // Default branch or fetch from context if possible
                    null,
                    null, // User ID not always available here without context, assume System or last logged user
                    "System",
                    "SYSTEM",
                    "LOYALTY_UPDATE",
                    "CUSTOMER",
                    customerId, // Already Long
                    "Updated loyalty points for customer " + customer.getName() + ": " + (points > 0 ? "+" : "") + points,
                    "{\"newBalance\":" + customer.getLoyaltyPoints() + "}"
                );
            } catch (Exception e) {
                // Ignore log error
            }
        });
    }
    
    public void requestLoyaltyRedemption(Long customerId, Integer pointsReq) {
        com.silverline.erp.domain.pos.Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
                
        if (customer.getLoyaltyPoints() == null || customer.getLoyaltyPoints() < 100) {
            throw new RuntimeException("Minimum 100 points required to redeem.");
        }
        if (customer.getLoyaltyPoints() < pointsReq) {
            throw new RuntimeException("Insufficient loyalty points limit.");
        }
        if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
            throw new RuntimeException("Customer email not configured. Please register customer email first.");
        }

        // Generate 4-digit OTP
        String otp = String.format("%04d", new java.util.Random().nextInt(10000));
        loyaltyOtpStore.put(customerId, otp);

        // Send Email
        String subject = "ROCS POS - Loyalty Points Redemption";
        String body = String.format(
                "Hello %s,\n\n" +
                "You have requested to redeem %d loyalty points at ROCS POS.\n" +
                "Your authorization code is: %s\n\n" +
                 "If this was not you, please contact support.\n\n" +
                 "Thank you,\nROCS System",
                customer.getName(), pointsReq, otp
        );
        emailService.sendSimpleMessage(customer.getEmail(), subject, body);
    }

    @Transactional
    public BigDecimal verifyLoyaltyRedemption(Long customerId, Integer pointsReq, String otpCode) {
        String storedOtp = loyaltyOtpStore.get(customerId);
        if (storedOtp == null || !storedOtp.equals(otpCode)) {
            throw new RuntimeException("Invalid or expired OTP code");
        }
        
        // Remove OTP early to prevent reuse
        loyaltyOtpStore.remove(customerId);

        com.silverline.erp.domain.pos.Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getLoyaltyPoints() == null || customer.getLoyaltyPoints() < pointsReq) {
            throw new RuntimeException("Customer does not have enough points.");
        }

        // Deduct points
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - pointsReq);
        customerRepository.save(customer);
        
        // 1 Point = 1 LKR mapped value
        return BigDecimal.valueOf(pointsReq);
    }

    private String generateCustomerCode(String phone) {
        return "CUS-" + phone; // Use phone number for informative ID
    }
    public List<com.silverline.erp.domain.pos.Customer> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<com.silverline.erp.domain.pos.Customer> byPhone = customerRepository.findByPhoneContaining(query);
        List<com.silverline.erp.domain.pos.Customer> byName = customerRepository.findByNameContainingIgnoreCase(query);
        
        // Merge without duplicates
        java.util.Set<com.silverline.erp.domain.pos.Customer> merged = new java.util.HashSet<>(byPhone);
        merged.addAll(byName);
        
        return new ArrayList<>(merged);
    }
    
    public com.silverline.erp.domain.pos.Customer getCustomerByCode(String code) {
        return customerRepository.findByCode(code).orElse(null);
    }

    @Transactional
    public Long processReturn(ReturnRequest request) {
        // --- Supervisor Validation ---
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
                UserProfile supervisor = userProfileRepo.findByUsername(supUser)
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
             throw new RuntimeException("Supervisor authorization failed: " + e.getMessage());
        }
        // -----------------------------

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
                 // item.setCondition(itemReq.getCondition());
                 
                 items.add(item);
                 totalRefund = totalRefund.add(item.getTotal());

                 // Restock inventory logic
                 // Restock inventory logic
                 try {
                     // Update Stock entity
                     Stock stock = stockRepository.findByBranchIdAndProductId(request.getBranchId(), itemReq.getProductId())
                             .orElse(null);
                     
                     if (stock != null) {
                         stock.setQuantity(stock.getQuantity().add(itemReq.getQty()));
                         stock.setAvailableQty(stock.getAvailableQty().add(itemReq.getQty()));
                         stockRepository.save(stock);
                     } else {
                         // Create new stock entry if not exists (less likely for return, but possible)
                         // For now, only update if exists or log warning
                         System.err.println("Stock not found for product " + itemReq.getProductId() + " in branch " + request.getBranchId());
                         
                         // Optional: Create new stock record
                         Stock newStock = new Stock();
                         newStock.setBranchId(request.getBranchId());
                         newStock.setProductId(itemReq.getProductId());
                         newStock.setQuantity(itemReq.getQty());
                         newStock.setAvailableQty(itemReq.getQty());
                         newStock.setReservedQty(BigDecimal.ZERO);
                         stockRepository.save(newStock);
                     }
                 } catch (Exception e) {
                     System.err.println("Failed to restock product " + itemReq.getProductId() + ": " + e.getMessage());
                 }
            }
            salesReturnItemRepository.saveAll(items);
        }
        
        ret.setTotalAmount(totalRefund);
        salesReturnRepository.save(ret);

        // Fetch supervisor username for logging
        String supervisorUsername = userProfileRepo.findById(supervisorId)
                .map(UserProfile::getUsername)
                .orElse("Supervisor #" + supervisorId);

        // Log Activity
        activityLogService.logActivity(
            request.getBranchId(),
            null,
            supervisorId, // Who authorized it
            supervisorUsername,
            "SUPERVISOR",
            "RETURN",
            "RETURN",
            ret.getReturnId(), // Use ID
            "Return processed for Sale #" + request.getSaleId() + ". Refund: " + totalRefund,
            "{\"itemCount\":" + items.size() + "}"
        );
        
        return ret.getReturnId();
    }

    private String generateReturnNo() {
        return "RET-" + System.currentTimeMillis();
    }
    
    public List<SaleSummaryDTO> getHeldBills(Long branchId) {
        // Fetch sales with status 'HELD' for branch
        // For now, assuming request filtering handles branch or all
        List<Sale> heldSales = saleRepository.findByPaymentStatus("HELD"); // Add branch filter in repo if needed
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return heldSales.stream().map(sale -> {
            // Filter by branch logic if not in repo
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
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional
    public void updateSaleStatus(Long saleId, String status) {
        saleRepository.findById(saleId).ifPresent(sale -> {
            sale.setPaymentStatus(status);
            saleRepository.save(sale);
        });
    }

    public List<ProductSalesHistoryDTO> getProductSalesHistory(Long productId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(java.time.LocalTime.MAX);

        List<ProductSalesHistoryDTO> results = saleItemRepository.findDailySalesByProduct(productId, start, end);
        // If repo returns DTO directly via constructor expression using Object, it's already mapped.
        // But verify if the aggregation returns valid list even if empty.
        if (results == null) return new ArrayList<>();
        return results;
    }
}



