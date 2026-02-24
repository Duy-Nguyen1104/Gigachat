package com.project.gigachat.service;

import com.project.gigachat.dto.websocket.WsEvent;
import com.project.gigachat.entity.ConversationParticipant;
import com.project.gigachat.entity.User;
import com.project.gigachat.repository.ConversationParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Centralised helper for sending STOMP events from backend services.
 *
 * <p>Destination conventions:
 * <ul>
 *   <li>{@code /topic/conversation/{id}} — messages, typing, participant changes</li>
 *   <li>{@code /user/{userId}/queue/notifications} — personal events (online status, read receipts)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationParticipantRepository participantRepository;

    // ── Broadcast to all subscribers of a conversation topic ────────────────

    public void broadcastToConversation(UUID conversationId, String type, Object data) {
        WsEvent event = WsEvent.builder().type(type).data(data).build();
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, event);
        log.debug("WS → /topic/conversation/{} [{}]", conversationId, type);
    }

    // ── Send to a specific user's private queue ──────────────────────────────

    public void sendToUser(UUID userId, String type, Object data) {
        WsEvent event = WsEvent.builder().type(type).data(data).build();
        // SimpMessagingTemplate.convertAndSendToUser automatically prefixes with /user
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", event);
        log.debug("WS → /user/{}/queue/notifications [{}]", userId, type);
    }

    // ── Broadcast online/offline status to conversation partners ────────────

    /**
     * When a user connects or disconnects, notify all other participants in
     * every conversation the user belongs to.
     */
    @Transactional(readOnly = true)
    public void broadcastUserStatusToPartners(User user, boolean online) {
        List<ConversationParticipant> participations = participantRepository.findByUser(user);

        Map<String, Object> data = Map.of(
                "userId", user.getId().toString(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "isOnline", online,
                "lastSeen", user.getLastSeen() != null ? user.getLastSeen().toString() : ""
        );

        String eventType = online ? "USER_ONLINE" : "USER_OFFLINE";

        participations.stream()
                .map(cp -> cp.getConversation().getId())
                .distinct()
                .forEach(convId -> broadcastToConversation(convId, eventType, data));

        log.info("Broadcast {} for user {} across {} conversation(s)",
                eventType, user.getId(), participations.size());
    }
}
