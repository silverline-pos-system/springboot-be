package com.silverline.erp.module.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silverline.erp.common.filter.JwtFilter;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.module.auth.dto.LogInRequestDTO;
import com.silverline.erp.module.auth.dto.LogInResponseDTO;
import com.silverline.erp.module.auth.dto.RegisterRequestDTO;
import com.silverline.erp.module.auth.dto.RegisterResponseDTO;
import com.silverline.erp.module.auth.service.AuthService;
import com.silverline.erp.module.auth.service.MyUserDetailsService;
import com.silverline.erp.module.auth.service.PasswordResetRequestService;
import com.silverline.erp.module.auth.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private PasswordResetRequestService passwordResetRequestService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private MyUserDetailsService userDetailsService;

    @Test
    public void registerUser_Success() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setFullName("Contract Test User");
        request.setUsername("contractUser");
        request.setEmail("contract@example.com");
        request.setPassword("password123");
        request.setPhone("1234567890");

        RegisterResponseDTO response = new RegisterResponseDTO(
                1L,
                "contract@example.com",
                "Contract Test User",
                null,
                AccountStatus.PENDING,
                "User registered successfully"
        );

        when(registrationService.registerUser(any(RegisterRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("contract@example.com"));
    }

    @Test
    public void registerUser_ValidationError() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        // leave required fields blank to trigger validation

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void login_Success() throws Exception {
        LogInRequestDTO request = new LogInRequestDTO();
        request.setUsername("contractUser");
        request.setPassword("password123");

        LogInResponseDTO response = new LogInResponseDTO(
                1L,
                "contractUser",
                "contract@example.com",
                "mock-jwt-token",
                null,
                null,
                null,
                "Login successful"
        );

        when(authService.logInUser("contractUser", "password123")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }
}
