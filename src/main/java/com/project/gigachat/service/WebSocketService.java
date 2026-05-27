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

    // Broadcast to active subscribers of a conversation topic, e.g. typing indicators.
    public void broadcastToConversation(UUID conversationId, String type, Object data) {
        WsEvent event = WsEvent.builder().type(type).data(data).build();
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, event);
        log.debug("WS -> /topic/conversation/{} [{}]", conversationId, type);
    }

    // Send to a specific user's private queue.
    public void sendToUser(UUID userId, String type, Object data) {
        WsEvent event = WsEvent.builder().type(type).data(data).build();
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", event);
        log.debug("WS -> /user/{}/queue/notifications [{}]", userId, type);
    }

    // Send personal notifications to every participant in a conversation.
    // This reaches logged-in users even before they open the conversation.
    @Transactional(readOnly = true)
    public void sendToConversationParticipants(UUID conversationId, String type, Object data) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);

        participants.forEach(participant -> sendToUser(participant.getUser().getId(), type, data));
        log.debug("WS -> {} participants in conversation {} [{}]", participants.size(), conversationId, type);
    }

    // Broadcast online/offline status to conversation partners.
    public void broadcastUserStatusToPartners(User user, boolean online) {
        List<ConversationParticipant> participants = participantRepository.findByUser(user);
        String eventType = online ? "USER_ONLINE" : "USER_OFFLINE";

        participants.stream()
                .map(cp -> cp.getConversation().getId())
                .distinct()
                .forEach(convId -> broadcastToConversation(convId, eventType, userStatusPayload(user, online)));

        log.info("Broadcast {} for user {} across {} conversation(s)",
                eventType, user.getId(), participants.size());
    }

    private Map<String, Object> userStatusPayload(User user, boolean online) {
        return Map.of(
                "userId", user.getId().toString(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "isOnline", online,
                "lastSeen", user.getLastSeen() != null ? user.getLastSeen().toString() : ""
        );
    }
}
