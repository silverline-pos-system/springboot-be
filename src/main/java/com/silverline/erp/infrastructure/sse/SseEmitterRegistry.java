package com.silverline.erp.infrastructure.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterRegistry {

    private final Map<SseChannel, Map<Long, List<SseEmitter>>> registry = new ConcurrentHashMap<>();

    public SseEmitterRegistry() {
        for (SseChannel channel : SseChannel.values()) {
            registry.put(channel, new ConcurrentHashMap<>());
        }
    }

    /**
     * Register a new SSE emitter for a user on a specific channel.
     */
    public void register(SseChannel channel, Long userId, SseEmitter emitter) {
        registry.get(channel)
                .computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);
        log.info("Registered SSE emitter on channel: {}, user ID: {}, active connections: {}",
                channel, userId, registry.get(channel).get(userId).size());

        emitter.onCompletion(() -> remove(channel, userId, emitter));
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out on channel: {}, user ID: {}", channel, userId);
            emitter.complete();
            remove(channel, userId, emitter);
        });
        emitter.onError((ex) -> {
            log.error("SSE emitter error on channel: {}, user ID: {}: {}", channel, userId, ex.getMessage());
            emitter.completeWithError(ex);
            remove(channel, userId, emitter);
        });
    }

    /**
     * Send payload to all active emitters of a specific user on a channel.
     */
    public void sendToUser(SseChannel channel, Long userId, Object payload) {
        sendToUser(channel, userId, "notification", payload);
    }

    /**
     * Send payload with custom event name to all active emitters of a specific user on a channel.
     */
    public void sendToUser(SseChannel channel, Long userId, String eventName, Object payload) {
        Map<Long, List<SseEmitter>> channelEmitters = registry.get(channel);
        List<SseEmitter> userEmitters = channelEmitters.get(userId);
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
                log.warn("Failed to send event on channel {} to user ID {}, removing emitter: {}", channel, userId, e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            userEmitters.removeAll(deadEmitters);
            if (userEmitters.isEmpty()) {
                channelEmitters.remove(userId);
            }
        }
    }

    /**
     * Broadcast payload to all connected SSE emitters on a channel.
     */
    public void broadcast(SseChannel channel, Object payload) {
        registry.get(channel).forEach((userId, list) -> sendToUser(channel, userId, "notification", payload));
    }

    /**
     * Broadcast payload with custom event name to all connected SSE emitters on a channel.
     */
    public void broadcast(SseChannel channel, String eventName, Object payload) {
        registry.get(channel).forEach((userId, list) -> sendToUser(channel, userId, eventName, payload));
    }

    /**
     * Send periodic keep-alive ping events to all connected clients across all channels
     * to prevent Nginx/Cloudflare and browser timeouts.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 15000)
    public void sendHeartbeat() {
        log.debug("Sending SSE heartbeat ping to all active clients across all channels");
        
        for (SseChannel channel : SseChannel.values()) {
            Map<Long, List<SseEmitter>> channelEmitters = registry.get(channel);
            if (channelEmitters.isEmpty()) {
                continue;
            }

            List<Long> emptyUsers = new ArrayList<>();

            channelEmitters.forEach((userId, userEmitters) -> {
                if (userEmitters == null || userEmitters.isEmpty()) {
                    emptyUsers.add(userId);
                    return;
                }

                List<SseEmitter> deadEmitters = new ArrayList<>();
                for (SseEmitter emitter : userEmitters) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("ping")
                                .data("heartbeat"));
                    } catch (IOException | IllegalStateException e) {
                        deadEmitters.add(emitter);
                    }
                }

                if (!deadEmitters.isEmpty()) {
                    userEmitters.removeAll(deadEmitters);
                    if (userEmitters.isEmpty()) {
                        emptyUsers.add(userId);
                    }
                }
            });

            for (Long userId : emptyUsers) {
                channelEmitters.remove(userId);
            }
        }
    }

    /**
     * Gracefully close all emitters on server shutdown.
     */
    @jakarta.annotation.PreDestroy
    public void cleanup() {
        log.info("Server shutting down. Gracefully completing all SSE emitters.");
        for (SseChannel channel : SseChannel.values()) {
            Map<Long, List<SseEmitter>> channelEmitters = registry.get(channel);
            channelEmitters.forEach((userId, userEmitters) -> {
                if (userEmitters != null) {
                    for (SseEmitter emitter : userEmitters) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("shutdown")
                                    .data("Server is shutting down. Please reconnect later."));
                            emitter.complete();
                        } catch (Exception e) {
                            // ignore failures during shutdown
                        }
                    }
                }
            });
            channelEmitters.clear();
        }
    }

    private void remove(SseChannel channel, Long userId, SseEmitter emitter) {
        Map<Long, List<SseEmitter>> channelEmitters = registry.get(channel);
        List<SseEmitter> userEmitters = channelEmitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                channelEmitters.remove(userId);
                log.info("Removed all SSE emitters on channel {} for user ID: {}", channel, userId);
            } else {
                log.info("Removed an SSE emitter on channel {} for user ID: {}, remaining: {}", channel, userId, userEmitters.size());
            }
        }
    }
}
