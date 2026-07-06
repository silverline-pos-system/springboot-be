package com.silverline.erp.common.config;

import com.silverline.erp.domain.enums.AccountStatus;
import com.silverline.erp.domain.enums.Role;
import com.silverline.erp.domain.system.SaasFeature;
import com.silverline.erp.domain.user.UserProfile;
import com.silverline.erp.module.admin.repository.SaasFeatureRepository;
import com.silverline.erp.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StartupConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SaasFeatureRepository saasFeatureRepository;

    @Value("${rocs.admin.username:admin}")
    private String adminUsername;

    @Value("${rocs.admin.password:admin@123}")
    private String adminPassword;

    @Value("${rocs.admin.email:shamikakeshanuni@gmail.com}")
    private String adminEmail;

    @Override
    public void run(String... args) throws Exception {
        createDefaultSuperAdmin();
        seedSaasFeatures();
    }

    private void createDefaultSuperAdmin() {
        if (userRepository.count() == 0) {
            log.info("No users found in the system. Creating default SUPER_ADMIN...");

            UserProfile admin = new UserProfile();
            admin.setFullName("System Super Admin");
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.SUPER_ADMIN);
            admin.setAccountStatus(AccountStatus.ACTIVE);
            admin.setEmployeeId("SUPER-001");
            admin.setCreatedAt(LocalDateTime.now());
            admin.setMustChangePassword(true);
            admin.setEmailVerified(true);

            userRepository.save(admin);
            log.info("Default SUPER_ADMIN created successfully!");
            log.info("Username: {}", adminUsername);
        } else {
            log.info("System already has users. Skipping default SUPER_ADMIN creation.");
        }
    }

    private void seedSaasFeatures() {
        if (saasFeatureRepository.count() == 0) {
            log.info("No SaaS features found. Seeding default features...");

            // Common Features (Active by default)
            saveFeature("SIMPLE_POS", "Simple POS", "COMMON", true);
            saveFeature("SIMPLE_INVENTORY", "Simple Inventory", "COMMON", true);
            saveFeature("SIMPLE_MANAGER", "Simple Manager", "COMMON", true);

            // Premium Features (Inactive by default)
            saveFeature("POS_LOYALTY", "POS Loyalty Program", "PREMIUM", false);
            saveFeature("POS_SERVICE_REPAIRS", "POS Service & Repairs", "PREMIUM", false);
            saveFeature("INVENTORY_EXPIRE_CALENDAR", "Inventory Expiry Calendar", "PREMIUM", false);
            saveFeature("MANAGER_LOYALTY", "Manager Loyalty Program", "PREMIUM", false);
            saveFeature("SALES_REPORTS", "Sales Reports & Analytics", "PREMIUM", false);
            saveFeature("MANAGER_ACCOUNTING", "Manager Accounting Module", "PREMIUM", false);
            saveFeature("ALLOW_OUT_OF_STOCK", "Allow Out Of Stock POS Sales", "PREMIUM", false);

            log.info("Default SaaS features seeded successfully!");
        }
    }

    private void saveFeature(String code, String name, String category, boolean isActive) {
        SaasFeature feature = new SaasFeature();
        feature.setFeatureCode(code);
        feature.setFeatureName(name);
        feature.setFeatureCategory(category);
        feature.setIsActive(isActive);
        saasFeatureRepository.save(feature);
    }
}
