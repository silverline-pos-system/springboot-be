package com.silverline.erp.common.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean serviceAvailable = checkExternalService();
        if (serviceAvailable) {
            return Health.up()
                    .withDetail("External ERP Integration", "Online")
                    .withDetail("Email Server", "Online")
                    .withDetail("SMS Gateway", "Online")
                    .build();
        }
        return Health.down()
                .withDetail("External ERP Integration", "Offline or unreachable")
                .build();
    }

    private boolean checkExternalService() {
        // Simulating health check logic for external gateway connection.
        // In real environments, this would perform a lightweight ping or connection check.
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
