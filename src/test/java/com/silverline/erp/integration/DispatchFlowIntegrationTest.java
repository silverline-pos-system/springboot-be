package com.silverline.erp.integration;

import com.silverline.erp.domain.branch.Branch;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.inventory.Dispatch;
import com.silverline.erp.domain.inventory.DispatchItem;
import com.silverline.erp.domain.inventory.Stock;
import com.silverline.erp.domain.inventory.Supplier;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.BranchRepository;
import com.silverline.erp.module.auth.repo.UserProfileRepo;
import com.silverline.erp.module.inventory.repository.ProductRepository;
import com.silverline.erp.module.inventory.repository.StockRepository;
import com.silverline.erp.module.inventory.repository.SupplierRepository;
import com.silverline.erp.module.procurement.dto.DispatchResponseDTO;
import com.silverline.erp.module.procurement.repository.DispatchItemRepository;
import com.silverline.erp.module.procurement.repository.DispatchRepository;
import com.silverline.erp.module.procurement.service.DispatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class DispatchFlowIntegrationTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private DispatchRepository dispatchRepository;

    @Autowired
    private DispatchItemRepository dispatchItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserProfileRepo userProfileRepo;

    @Test
    public void testApproveDispatch_Success() {
        // 1. Create and persist Branch
        Branch branch = new Branch();
        branch.setName("Procurement Branch");
        branch.setCode("BR_PROC_001");
        branch.setIsActive(true);
        branch = branchRepository.save(branch);

        // 2. Create and persist User (Manager/Admin)
        UserProfile user = new UserProfile();
        user.setUsername("procManager");
        user.setEmail("procManager@example.com");
        user.setPassword("password");
        user.setFullName("Procurement Manager");
        user.setEmployeeId("EMP_PM_001");
        user.setRole(Role.MANAGER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user = userProfileRepo.save(user);

        // 3. Create and persist Supplier
        Supplier supplier = new Supplier();
        supplier.setCode("SUP_001");
        supplier.setName("Main Supplier");
        supplier.setIsActive(true);
        supplier = supplierRepository.save(supplier);

        // 4. Create and persist Product
        Product product = new Product();
        product.setSku("PROD_DISP_001");
        product.setBarcode("9876543212");
        product.setName("Dispatch Product");
        product.setCostPrice(BigDecimal.valueOf(40));
        product.setSellingPrice(BigDecimal.valueOf(80));
        product.setIsActive(true);
        product = productRepository.save(product);

        // 5. Create and persist Dispatch
        Dispatch dispatch = new Dispatch();
        dispatch.setDispatchNo("DSP-001");
        dispatch.setBranchId(branch.getBranchId());
        dispatch.setSupplierId(supplier.getSupplierId());
        dispatch.setDispatchDate(LocalDate.now());
        dispatch.setStatus("PENDING");
        dispatch.setCreatedBy(user.getUserId());
        dispatch = dispatchRepository.save(dispatch);

        // 6. Create and persist DispatchItem
        DispatchItem item = new DispatchItem();
        item.setDispatchId(dispatch.getDispatchId());
        item.setProductId(product.getProductId());
        item.setToBranchId(branch.getBranchId());
        item.setQtyDispatched(BigDecimal.valueOf(20));
        item.setUnitPrice(BigDecimal.valueOf(40));
        item.setSellingPrice(BigDecimal.valueOf(80));
        item.setMrp(BigDecimal.valueOf(90));
        item = dispatchItemRepository.save(item);

        // 7. Approve the dispatch
        DispatchResponseDTO response = dispatchService.approveDispatch(dispatch.getDispatchId(), user.getUserId());

        // 8. Asserts & Verification
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals(user.getUserId(), response.getApprovedBy());

        // Verify stock is updated/created (0 + 20 = 20)
        Stock stock = stockRepository.findByBranchIdAndProductId(branch.getBranchId(), product.getProductId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(20).compareTo(stock.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(stock.getAvailableQty()));
    }
}
