package com.silverline.erp.module.admin.service;

import com.silverline.erp.common.exception.DuplicateResourceException;
import com.silverline.erp.common.exception.ValidationException;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.user.SecondaryRoleAssignment;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.auth.repository.UserRepository;
import com.silverline.erp.module.manager.dto.AssignSecondaryRoleRequest;
import com.silverline.erp.module.manager.dto.SecondaryRoleAssignmentDTO;
import com.silverline.erp.module.manager.repository.SecondaryRoleAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecondaryRoleServiceTest {

    @Mock
    private SecondaryRoleAssignmentRepository assignmentRepo;
    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private SecondaryRoleService service;

    private UserProfile cashier() {
        UserProfile u = new UserProfile();
        u.setUserId(5L);
        u.setUsername("cashier1");
        u.setRole(Role.CASHIER);
        return u;
    }

    private AssignSecondaryRoleRequest request(String role, String expiresAtIso) {
        AssignSecondaryRoleRequest r = new AssignSecondaryRoleRequest();
        r.setUserId(5L);
        r.setSecondaryRole(role);
        r.setExpiresAt(expiresAtIso);
        r.setReason("cover shift");
        return r;
    }

    @Test
    void assignRole_convertsUtcExpiryToServerLocalTime() {
        // The frontend sends UTC (JS toISOString). The stored expiry must be that INSTANT converted to
        // the server zone, not the UTC wall-clock reinterpreted as local (the old bug).
        String utc = "2030-06-15T18:30:00.000Z";
        LocalDateTime expectedLocal = OffsetDateTime.parse(utc)
                .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        when(userRepo.findById(5L)).thenReturn(Optional.of(cashier()));
        when(assignmentRepo.existsByUserIdAndRevokedFalseAndExpiresAtAfter(eq(5L), any())).thenReturn(false);
        when(assignmentRepo.save(any(SecondaryRoleAssignment.class))).thenAnswer(inv -> {
            SecondaryRoleAssignment a = inv.getArgument(0);
            a.setId(1L);
            a.setCreatedAt(LocalDateTime.now());
            return a;
        });

        SecondaryRoleAssignmentDTO dto = service.assignRole(request("STORE_KEEPER", utc));

        ArgumentCaptor<SecondaryRoleAssignment> captor = ArgumentCaptor.forClass(SecondaryRoleAssignment.class);
        org.mockito.Mockito.verify(assignmentRepo).save(captor.capture());
        assertEquals(expectedLocal, captor.getValue().getExpiresAt());
        assertEquals("STORE_KEEPER", dto.getSecondaryRole());
    }

    @Test
    void assignRole_rejectsInvalidRole() {
        assertThrows(ValidationException.class, () ->
                service.assignRole(request("GOD_MODE", "2030-06-15T18:30:00.000Z")));
    }

    @Test
    void assignRole_rejectsSameAsPrimary() {
        when(userRepo.findById(5L)).thenReturn(Optional.of(cashier()));
        assertThrows(ValidationException.class, () ->
                service.assignRole(request("CASHIER", "2030-06-15T18:30:00.000Z")));
    }

    @Test
    void assignRole_rejectsPastExpiry() {
        when(userRepo.findById(5L)).thenReturn(Optional.of(cashier()));
        assertThrows(ValidationException.class, () ->
                service.assignRole(request("STORE_KEEPER", "2000-01-01T00:00:00.000Z")));
    }

    @Test
    void assignRole_rejectsWhenActiveAssignmentExists() {
        when(userRepo.findById(5L)).thenReturn(Optional.of(cashier()));
        when(assignmentRepo.existsByUserIdAndRevokedFalseAndExpiresAtAfter(eq(5L), any())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () ->
                service.assignRole(request("STORE_KEEPER", "2030-06-15T18:30:00.000Z")));
    }
}
