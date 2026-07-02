package com.silverline.erp.integration;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.pos.Sale;
import com.silverline.erp.domain.pos.SalesReturn;
import com.silverline.erp.domain.pos.SalesReturnItem;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.StockRepository;
import com.silverline.erp.module.pos.dto.returns.ReturnRequest;
import com.silverline.erp.module.pos.repository.CustomerRepository;
import com.silverline.erp.module.pos.repository.SaleRepository;
import com.silverline.erp.module.pos.repository.SalesReturnItemRepository;
import com.silverline.erp.module.pos.repository.SalesReturnRepository;
import com.silverline.erp.module.pos.service.ReturnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ReturnFlowIntegrationTest {

    @Autowired
    private ReturnService returnService;

    @Autowired
    private SalesReturnRepository salesReturnRepository;

    @Autowired
    private SalesReturnItemRepository salesReturnItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserProfileRepo userProfileRepo;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testProcessReturn_Success() {
        // 1. Create and persist Branch
        Branch branch = new Branch();
        branch.setName("Return Test Branch");
        branch.setCode("BR_RET_001");
        branch.setIsActive(true);
        branch = branchRepository.save(branch);

        // 2. Create and persist Supervisor User (using real bcrypt password)
        UserProfile supervisor = new UserProfile();
        supervisor.setUsername("supervisorTest");
        supervisor.setEmail("supervisorTest@example.com");
        supervisor.setPassword(passwordEncoder.encode("superPassword123"));
        supervisor.setFullName("Test Supervisor");
        supervisor.setEmployeeId("EMP_SUP_001");
        supervisor.setRole(Role.SUPER_ADMIN);
        supervisor.setAccountStatus(AccountStatus.ACTIVE);
        supervisor = userProfileRepo.save(supervisor);

        // 3. Create and persist Product
        Product product = new Product();
        product.setSku("PROD_RET_001");
        product.setBarcode("9876543211");
        product.setName("Return Test Product");
        product.setCostPrice(BigDecimal.valueOf(50));
        product.setSellingPrice(BigDecimal.valueOf(100));
        product.setIsActive(true);
        Product savedProduct = productRepository.save(product);

        // 4. Setup initial Stock
        Stock stock = new Stock();
        stock.setBranchId(branch.getBranchId());
        stock.setProductId(savedProduct.getProductId());
        stock.setQuantity(BigDecimal.valueOf(5));
        stock.setAvailableQty(BigDecimal.valueOf(5));
        stock.setReservedQty(BigDecimal.ZERO);
        stock = stockRepository.save(stock);

        // 5. Create a completed Sale record
        Sale sale = new Sale();
        sale.setBranchId(branch.getBranchId());
        sale.setInvoiceNo("INV-RET-001");
        sale.setGrossTotal(BigDecimal.valueOf(100));
        sale.setNetTotal(BigDecimal.valueOf(100));
        sale.setPaymentStatus("PAID");
        sale.setSaleDate(LocalDateTime.now());
        Long saleId = saleRepository.save(sale);
        sale.setSaleId(saleId);

        // 6. Prepare ReturnRequest
        ReturnRequest request = new ReturnRequest();
        request.setSaleId(saleId);
        request.setBranchId(branch.getBranchId());
        request.setReason("Item defective");
        request.setRefundMethod("CASH");
        request.setSupervisorUsername("supervisorTest");
        request.setSupervisorPassword("superPassword123");

        ReturnRequest.ReturnItemRequest itemReq = new ReturnRequest.ReturnItemRequest();
        itemReq.setSaleItemId(999L); // Mock sale item id
        itemReq.setProductId(product.getProductId());
        itemReq.setQty(BigDecimal.valueOf(2));
        itemReq.setUnitPrice(BigDecimal.valueOf(100));
        request.setItems(Collections.singletonList(itemReq));

        // 7. Execute Process Return
        Long returnId = returnService.processReturn(request);
        assertNotNull(returnId);

        // 8. Asserts & Verification
        SalesReturn salesReturn = salesReturnRepository.findById(returnId).orElseThrow();
        assertEquals("APPROVED", salesReturn.getStatus());
        assertEquals(0, BigDecimal.valueOf(200.00).compareTo(salesReturn.getTotalAmount()));

        // Verify return items are persisted
        List<SalesReturnItem> returnItems = salesReturnItemRepository.findAll();
        boolean found = returnItems.stream().anyMatch(i -> i.getSalesReturn().getReturnId().equals(returnId)
                && i.getProductId().equals(savedProduct.getProductId()));
        assertTrue(found);

        // Verify stock is replenished (5 + 2 = 7)
        Stock updatedStock = stockRepository.findByBranchIdAndProductId(branch.getBranchId(), savedProduct.getProductId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(7).compareTo(updatedStock.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(7).compareTo(updatedStock.getAvailableQty()));
    }
}
