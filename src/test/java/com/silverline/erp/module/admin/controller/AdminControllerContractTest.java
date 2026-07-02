package com.silverline.erp.module.admin.controller;

import com.silverline.erp.common.filter.JwtFilter;
import com.silverline.erp.module.admin.dto.UserDTO;
import com.silverline.erp.module.admin.service.SaasFeatureService;
import com.silverline.erp.module.admin.service.UserService;
import com.silverline.erp.module.auth.service.MyUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, SystemConfigController.class})
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SaasFeatureService saasFeatureService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private MyUserDetailsService userDetailsService;

    @Test
    public void getAllUsers_Success() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("adminUser");
        userDTO.setEmail("admin@example.com");

        when(userService.getAllUsers()).thenReturn(Collections.singletonList(userDTO));

        mockMvc.perform(get("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("adminUser"))
                .andExpect(jsonPath("$[0].email").value("admin@example.com"));
    }

    @Test
    public void getSystemName_Success() throws Exception {
        when(saasFeatureService.getSystemName()).thenReturn("Silverline ERP");

        mockMvc.perform(get("/api/v1/system/name")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemName").value("Silverline ERP"));
    }
}
