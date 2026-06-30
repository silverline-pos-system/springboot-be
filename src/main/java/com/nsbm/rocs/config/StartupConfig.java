package com.nsbm.rocs.config;

import com.nsbm.rocs.entity.enums.AccountStatus;
import com.nsbm.rocs.entity.enums.Role;
import com.nsbm.rocs.entity.main.UserProfile;
import com.nsbm.rocs.repository.UserRepository;
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

    @Value("${rocs.admin.username:admin}")
    private String adminUsername;

    @Value("${rocs.admin.password:admin@123}")
    private String adminPassword;

    @Value("${rocs.admin.email:shamikakeshanuni@gmail.com}")
    private String adminEmail;

    @Override
    public void run(String... args) throws Exception {
        createDefaultSuperAdmin();
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
}
