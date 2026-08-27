package com.silverline.erp.common.filter;

import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.SecondaryRoleAssignment;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.service.JwtService;
import com.silverline.erp.module.auth.service.MyUserDetailsService;
import com.silverline.erp.module.manager.repository.SecondaryRoleAssignmentRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock private JwtService jwtService;
    @Mock private MyUserDetailsService userDetailsService;
    @Mock private SecondaryRoleAssignmentRepository secondaryRoleRepo;

    @InjectMocks private JwtFilter jwtFilter;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private UserProfile cashier() {
        UserProfile u = new UserProfile();
        u.setUserId(7L);
        u.setUsername("cashier1");
        u.setRole(Role.CASHIER);
        return u;
    }

    @Test
    void activeSecondaryRole_isAddedToAuthorities() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        when(jwtService.extractUsername("token123")).thenReturn("cashier1");
        when(userDetailsService.loadUserByUsername("cashier1")).thenReturn(cashier());
        when(jwtService.validateToken(eq("token123"), any())).thenReturn(true);

        SecondaryRoleAssignment active = new SecondaryRoleAssignment();
        active.setSecondaryRole("STORE_KEEPER");
        active.setExpiresAt(LocalDateTime.now().plusHours(2));
        when(secondaryRoleRepo.findFirstByUserIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(eq(7L), any()))
                .thenReturn(Optional.of(active));

        jwtFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "authentication should be set");
        var roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        assertTrue(roles.contains("ROLE_CASHIER"), "primary role kept");
        assertTrue(roles.contains("ROLE_STORE_KEEPER"), "active secondary role granted");
    }

    @Test
    void noSecondaryRole_onlyPrimaryAuthority() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        when(jwtService.extractUsername("token123")).thenReturn("cashier1");
        when(userDetailsService.loadUserByUsername("cashier1")).thenReturn(cashier());
        when(jwtService.validateToken(eq("token123"), any())).thenReturn(true);
        when(secondaryRoleRepo.findFirstByUserIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(eq(7L), any()))
                .thenReturn(Optional.empty());

        jwtFilter.doFilter(req, res, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        var roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        assertTrue(roles.contains("ROLE_CASHIER"));
        assertFalse(roles.contains("ROLE_STORE_KEEPER"));
    }
}
