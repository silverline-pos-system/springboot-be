package com.silverline.erp.module.admin.event;

import com.silverline.erp.common.audit.repository.PasswordResetRequestRepository;
import com.silverline.erp.infrastructure.sse.SseChannel;
import com.silverline.erp.infrastructure.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEventListener {

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * Handle the event asynchronously so it doesn't block the main database transaction thread.
     */
    @Async
    @EventListener
    public void handlePasswordResetCountChanged(PasswordResetCountChangedEvent event) {
        try {
            long count = passwordResetRequestRepository.countByStatus("PENDING");
            log.info("Broadcasting updated pending password reset count asynchronously: {}", count);
            sseEmitterRegistry.broadcast(SseChannel.PASSWORD_RESETS, Map.of("pendingCount", count));
        } catch (Exception e) {
            log.error("Failed to broadcast updated pending count on event: {}", e.getMessage(), e);
        }
    }
}
