package com.silverline.erp.module.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.pos.dto.ShiftStartRequest;
import com.silverline.erp.module.pos.dto.sale.CreateSaleRequest;
import com.silverline.erp.module.pos.dto.sale.SaleItemRequest;
import com.silverline.erp.module.pos.dto.sale.SaleResponse;
import com.silverline.erp.module.pos.service.*;
import com.silverline.erp.common.filter.JwtFilter;
import com.silverline.erp.module.auth.service.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PosController.class, ShiftController.class})
@AutoConfigureMockMvc(addFilters = false)
public class PosControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PosSaleService saleService;

    @MockitoBean
    private ReturnService returnService;

    @MockitoBean
    private LoyaltyService loyaltyService;

    @MockitoBean
    private SaleQueryService saleQueryService;

    @MockitoBean
    private ShiftService shiftService;

    @MockitoBean
    private CashReconciliationService cashReconciliationService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private MyUserDetailsService userDetailsService;

    @BeforeEach
    public void setUp() {
        UserProfile mockUser = new UserProfile();
        mockUser.setUserId(123L);
        mockUser.setUsername("cashierUser");

        Authentication auth = new UsernamePasswordAuthenticationToken(mockUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void submitOrder_Success() throws Exception {
        CreateSaleRequest request = new CreateSaleRequest();
        request.setBranchId(1L);
        
        SaleItemRequest item = new SaleItemRequest();
        item.setProductId(100L);
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(BigDecimal.TEN);
        request.setItems(Collections.singletonList(item));

        SaleResponse response = new SaleResponse.Builder()
                .saleId(1L)
                .invoiceNo("INV-001")
                .paymentStatus("PAID")
                .netTotal(BigDecimal.TEN)
                .build();

        when(shiftService.getActiveShiftId(anyLong())).thenReturn(10L);
        when(saleService.createSale(any(CreateSaleRequest.class), anyLong(), anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/v1/pos/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order created successfully"))
                .andExpect(jsonPath("$.data.saleId").value(1L));
    }

    @Test
    public void submitOrder_ValidationError_EmptyItems() throws Exception {
        CreateSaleRequest request = new CreateSaleRequest();
        request.setBranchId(1L);
        request.setItems(Collections.emptyList()); // Empty items should fail validation

        mockMvc.perform(post("/api/v1/pos/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void startShift_ValidationError_MissingBranch() throws Exception {
        ShiftStartRequest request = new ShiftStartRequest();
        request.setCashierId(123L);
        request.setOpeningCash(BigDecimal.TEN);
        // branchId is not set to trigger validation error

        mockMvc.perform(post("/api/v1/pos/shift/open")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
