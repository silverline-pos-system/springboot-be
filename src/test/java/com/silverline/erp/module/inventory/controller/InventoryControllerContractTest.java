package com.silverline.erp.module.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silverline.erp.module.inventory.dto.ProductDTO;
import com.silverline.erp.module.inventory.dto.StockDTO;
import com.silverline.erp.module.inventory.service.ProductService;
import com.silverline.erp.module.inventory.service.StockService;
import com.silverline.erp.common.filter.JwtFilter;
import com.silverline.erp.module.auth.service.MyUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductController.class, StockController.class})
@AutoConfigureMockMvc(addFilters = false)
public class InventoryControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private StockService stockService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private MyUserDetailsService userDetailsService;

    @Test
    public void createProduct_Success() throws Exception {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setSku("PRD-INV-001");
        productDTO.setName("Contract Product");
        productDTO.setCostPrice(BigDecimal.valueOf(10));
        productDTO.setSellingPrice(BigDecimal.valueOf(15));
        productDTO.setIsActive(true);

        when(productService.createProduct(any(ProductDTO.class))).thenReturn(productDTO);

        mockMvc.perform(post("/api/inventory/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.sku").value("PRD-INV-001"));
    }

    @Test
    public void createProduct_ValidationError_MissingName() throws Exception {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setSku("PRD-INV-001");
        // name is not set, which triggers validation error

        mockMvc.perform(post("/api/inventory/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void getStockByBranch_Success() throws Exception {
        StockDTO stockDTO = new StockDTO();
        stockDTO.setStockId(1L);
        stockDTO.setBranchId(2L);
        stockDTO.setProductSku("PRD-INV-001");
        stockDTO.setQuantity(BigDecimal.valueOf(50));
        stockDTO.setAvailableQty(BigDecimal.valueOf(50));

        Page<StockDTO> pageInfo = new PageImpl<>(Collections.singletonList(stockDTO), PageRequest.of(0, 20), 1);

        when(stockService.getStockByBranch(anyLong(), any(Pageable.class))).thenReturn(pageInfo);

        mockMvc.perform(get("/api/inventory/stock/branch/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Stock retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].productSku").value("PRD-INV-001"))
                .andExpect(jsonPath("$.data.content[0].quantity").value(50));
    }
}
