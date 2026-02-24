package com.project.gigachat.websocket;

import com.project.gigachat.entity.User;
import com.project.gigachat.repository.ConversationParticipantRepository;
import com.project.gigachat.repository.ConversationRepository;
import com.project.gigachat.repository.UserRepository;
import com.project.gigachat.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Handles client-to-server STOMP messages.
 *
 * <p>Destinations (after /app prefix):
 * <ul>
 *   <li>{@code /typing.start} — user started typing in a conversation</li>
 *   <li>{@code /typing.stop}  — user stopped typing</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketService webSocketService;
    private final UserRepository userRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationRepository conversationRepository;

    /** Client sends: { "conversationId": "..." } */
    @MessageMapping("/typing.start")
    public void handleTypingStart(@Payload Map<String, String> payload, Principal principal) {
        User user = resolveUser(principal);
        if (user == null) return;
        String conversationId = payload.get("conversationId");
        if (conversationId == null) return;

        webSocketService.broadcastToConversation(
                UUID.fromString(conversationId),
                "TYPING_START",
                Map.of(
                        "conversationId", conversationId,
                        "userId", user.getId().toString(),
                        "displayName", user.getDisplayName() != null
                                ? user.getDisplayName() : user.getUsername()
                )
        );
    }

    /** Client sends: { "conversationId": "..." } */
    @MessageMapping("/typing.stop")
    public void handleTypingStop(@Payload Map<String, String> payload, Principal principal) {
        User user = resolveUser(principal);
        if (user == null) return;
        String conversationId = payload.get("conversationId");
        if (conversationId == null) return;

        webSocketService.broadcastToConversation(
                UUID.fromString(conversationId),
                "TYPING_STOP",
                Map.of(
                        "conversationId", conversationId,
                        "userId", user.getId().toString()
                )
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User resolveUser(Principal principal) {
        if (principal == null) return null;
        String email = null;
        if (principal instanceof Authentication auth
                && auth.getPrincipal() instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = principal.getName();
        }
        return userRepository.findByEmail(email).orElse(null);
    }
}
