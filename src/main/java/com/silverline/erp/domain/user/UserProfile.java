package com.silverline.erp.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_profiles")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // NOTE: branch_id and userBranches REMOVED â€” users are NOT assigned to branches

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 200)
    @JsonIgnore // Never serialize the BCrypt hash in any API response (SEC-06)
    private String password;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @Column(unique = true, length = 50)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Role role; // Assigned ONLY after approval

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus accountStatus;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    @JsonIgnore
    private UserProfile approvedBy;

    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private int failedLoginAttempts;
    private LocalDateTime lastLogin;
    private boolean mustChangePassword;

    @Column(unique = true, length = 100)
    private String registrationToken;

    private boolean emailVerified;
    private LocalDateTime emailVerifiedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.accountStatus == null) {
            this.accountStatus = AccountStatus.PENDING;
        }
        this.emailVerified = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == AccountStatus.ACTIVE;
    }
}
