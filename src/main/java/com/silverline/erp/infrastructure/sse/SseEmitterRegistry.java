package com.silverline.erp.infrastructure.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterRegistry {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE emitter for a user.
     */
    public void register(Long userId, SseEmitter emitter) {
        emitters.computeIfAbsent(userId, k -> new ArrayList<>()).add(emitter);
        log.info("Registered SSE emitter for user ID: {}, active connections: {}", userId, emitters.get(userId).size());

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out for user ID: {}", userId);
            emitter.complete();
            remove(userId, emitter);
        });
        emitter.onError((ex) -> {
            log.error("SSE emitter error for user ID: {}: {}", userId, ex.getMessage());
            emitter.completeWithError(ex);
            remove(userId, emitter);
        });
    }

    /**
     * Send payload to all active emitters of a specific user.
     */
    public void sendToUser(Long userId, Object payload) {
        sendToUser(userId, "notification", payload);
    }

    /**
     * Send payload with custom event name to all active emitters of a specific user.
     */
    public void sendToUser(Long userId, String eventName, Object payload) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload));
            } catch (IOException | IllegalStateException e) {
                log.warn("Failed to send event to user ID {}, removing emitter: {}", userId, e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            userEmitters.removeAll(deadEmitters);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * Broadcast payload to all connected SSE emitters.
     */
    public void broadcast(Object payload) {
        emitters.forEach((userId, list) -> sendToUser(userId, "notification", payload));
    }

    /**
     * Broadcast payload with custom event name to all connected SSE emitters.
     */
    public void broadcast(String eventName, Object payload) {
        emitters.forEach((userId, list) -> sendToUser(userId, eventName, payload));
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
                log.info("Removed all SSE emitters for user ID: {}", userId);
            } else {
                log.info("Removed an SSE emitter for user ID: {}, remaining: {}", userId, userEmitters.size());
            }
        }
    }
}
