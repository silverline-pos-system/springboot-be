package com.silverline.erp.integration;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.pos.CashShift;
import com.silverline.erp.domain.pos.Customer;
import com.silverline.erp.domain.pos.Payment;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.StockRepository;
import com.silverline.erp.module.pos.dto.sale.CreateSaleRequest;
import com.silverline.erp.module.pos.dto.sale.PaymentRequest;
import com.silverline.erp.module.pos.dto.sale.SaleItemRequest;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import com.silverline.erp.module.pos.repository.PaymentRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.module.pos.repository.ShiftRepository;
import com.silverline.erp.module.pos.service.PosSaleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class SaleFlowIntegrationTest {

    @Autowired
    private PosSaleService posSaleService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserProfileRepo userProfileRepo;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private StockRepository stockRepository;

    @Test
    public void testFullSaleFlow_Success() {
        // 1. Create and persist Branch
        Branch branch = new Branch();
        branch.setName("Test Branch");
        branch.setCode("BR_SALE_FLOW_001");
        branch.setIsActive(true);
        branch = branchRepository.save(branch);

        // 2. Create and persist Cashier User
        UserProfile cashier = new UserProfile();
        cashier.setUsername("cashierSaleFlow");
        cashier.setEmail("cashierSaleFlow@example.com");
        cashier.setPassword("password");
        cashier.setFullName("Test Cashier");
        cashier.setEmployeeId("EMP_SALE_FLOW_001");
        cashier.setRole(Role.CASHIER);
        cashier.setAccountStatus(AccountStatus.ACTIVE);
        cashier = userProfileRepo.save(cashier);

        // 3. Create and persist Customer
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setPhone("9999999999");
        customer.setCode("CUST001");
        customer.setIsActive(true);
        customer = customerRepository.save(customer);

        // 4. Create and persist Product
        Product product = new Product();
        product.setSku("PROD001");
        product.setBarcode("9876543210");
        product.setName("Integration Test Product");
        product.setCostPrice(BigDecimal.valueOf(50));
        product.setSellingPrice(BigDecimal.valueOf(100));
        product.setIsActive(true);
        product = productRepository.save(product);

        // 5. Setup initial Stock
        Stock stock = new Stock();
        stock.setBranchId(branch.getBranchId());
        stock.setProductId(product.getProductId());
        stock.setQuantity(BigDecimal.valueOf(10));
        stock.setAvailableQty(BigDecimal.valueOf(10));
        stock.setReservedQty(BigDecimal.ZERO);
        stockRepository.save(stock);

        // 6. Create active Shift
        CashShift shift = new CashShift();
        shift.setShiftNo("SHIFT-001");
        shift.setBranchId(branch.getBranchId());
        shift.setCashierId(cashier.getUserId());
        shift.setStatus(CashShift.ShiftStatus.OPEN);
        shift.setOpeningCash(BigDecimal.valueOf(200));
        shift.setOpenedAt(LocalDateTime.now());
        Long shiftId = shiftRepository.save(shift);
        shift.setShiftId(shiftId);

        // 7. Prepare Sale Request
        CreateSaleRequest request = new CreateSaleRequest();
        request.setBranchId(branch.getBranchId());
        request.setCustomerId(customer.getCustomerId());
        request.setDiscount(BigDecimal.ZERO);
        request.setNotes("Integration test sale");
        request.setStatus("PAID");

        SaleItemRequest itemRequest = new SaleItemRequest();
        itemRequest.setProductId(product.getProductId());
        itemRequest.setQuantity(BigDecimal.valueOf(2));
        itemRequest.setUnitPrice(BigDecimal.valueOf(100));
        itemRequest.setDiscount(BigDecimal.ZERO);
        request.setItems(Collections.singletonList(itemRequest));

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentType("CASH");
        paymentRequest.setAmount(BigDecimal.valueOf(200));
        request.setPayments(Collections.singletonList(paymentRequest));

        // 8. Execute Sale Creation
        SaleResponse response = posSaleService.createSale(request, branch.getBranchId(), cashier.getUserId(), shift.getShiftId());

        // 9. Asserts & Verification
        assertNotNull(response);
        assertNotNull(response.getSaleId());
        assertEquals("PAID", response.getPaymentStatus());
        assertEquals(0, BigDecimal.valueOf(200.00).compareTo(response.getNetTotal()));

        // Verify stock is updated/deducted
        Stock updatedStock = stockRepository.findByBranchIdAndProductId(branch.getBranchId(), product.getProductId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(8).compareTo(updatedStock.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(8).compareTo(updatedStock.getAvailableQty()));

        // Verify sale is persisted
        Sale persistedSale = saleRepository.findById(response.getSaleId()).orElseThrow();
        assertEquals(branch.getBranchId(), persistedSale.getBranchId());
        assertEquals(customer.getCustomerId(), persistedSale.getCustomerId());
        assertEquals(0, BigDecimal.valueOf(200.00).compareTo(persistedSale.getNetTotal()));

        // Verify payments are persisted
        List<Payment> payments = paymentRepository.findBySaleId(response.getSaleId());
        assertEquals(1, payments.size());
        assertEquals("CASH", payments.getFirst().getPaymentType());
        assertEquals(0, BigDecimal.valueOf(200.00).compareTo(payments.getFirst().getAmount()));
    }
}
