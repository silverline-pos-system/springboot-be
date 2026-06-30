package com.silverline.erp.domain.system;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "print_header_footer_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrintHeaderFooterSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false, unique = true)
    private Long branchId;

    // ===== HEADER FIELDS =====

    @Column(name = "header_business_name", length = 150)
    private String headerBusinessName;

    @Column(name = "header_branch_line", length = 150)
    private String headerBranchLine;

    @Column(name = "header_address", length = 255)
    private String headerAddress;

    @Column(name = "header_contact", length = 100)
    private String headerContact;

    @Column(name = "header_extra_line", length = 255)
    private String headerExtraLine;

    // ===== FOOTER FIELDS =====

    @Column(name = "footer_thank_you_line", length = 255)
    private String footerThankYouLine;

    @Column(name = "footer_policy_line", length = 255)
    private String footerPolicyLine;

    @Column(name = "footer_powered_by_line", length = 255)
    private String footerPoweredByLine;

    @Column(name = "footer_extra_line", length = 255)
    private String footerExtraLine;

    // ===== AUDIT =====

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

