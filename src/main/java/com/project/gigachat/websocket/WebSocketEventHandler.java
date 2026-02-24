package com.project.gigachat.websocket;

import com.project.gigachat.entity.User;
import com.project.gigachat.repository.UserRepository;
import com.project.gigachat.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for STOMP session lifecycle events to maintain online-user tracking
 * and broadcast presence changes to conversation participants.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventHandler {

    /** sessionId → userId (UUID) for active connections. */
    private final ConcurrentHashMap<String, UUID> sessionUserMap = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final WebSocketService webSocketService;

    // ── User connects ────────────────────────────────────────────────────────

    @EventListener
    @Transactional
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        if (principal == null || sessionId == null) return;

        String email = extractEmail(principal);
        userRepository.findByEmail(email).ifPresent(user -> {
            sessionUserMap.put(sessionId, user.getId());
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
            webSocketService.broadcastUserStatusToPartners(user, true);
            log.info("User {} connected (session {})", user.getId(), sessionId);
        });
    }

    // ── User disconnects ─────────────────────────────────────────────────────

    @EventListener
    @Transactional
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        UUID userId = sessionUserMap.remove(sessionId);
        if (userId == null) return;

        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
            webSocketService.broadcastUserStatusToPartners(user, false);
            log.info("User {} disconnected (session {})", userId, sessionId);
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractEmail(Principal principal) {
        if (principal instanceof Authentication auth) {
            Object p = auth.getPrincipal();
            if (p instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                return ud.getUsername(); // our UserDetailsService stores email as username
            }
        }
        return principal.getName();
    }
}
