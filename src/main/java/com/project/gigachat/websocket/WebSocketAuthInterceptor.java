package com.project.gigachat.websocket;

import com.project.gigachat.entity.User;
import com.project.gigachat.repository.UserRepository;
import com.project.gigachat.security.CustomUserDetailsService;
import com.project.gigachat.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Intercepts STOMP CONNECT frames and authenticates the user via the
 * "Authorization: Bearer <token>" header sent by the client.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        authenticate(accessor);
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return;
        }

        String authToken = authHeader.substring(BEARER_PREFIX.length());
        try {
            String email = jwtTokenProvider.extractUsername(authToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtTokenProvider.validateToken(authToken, userDetails)) {
                return;
            }

            User user = userRepository.findByEmail(email).orElseThrow();
            // Use the application user id as the STOMP Principal name so
            // convertAndSendToUser(userId, ...) resolves to this session.
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user.getId().toString(), null, userDetails.getAuthorities());
            accessor.setUser(auth);
            log.debug("WebSocket authenticated: {} ({})", email, user.getId());
        } catch (Exception e) {
            log.warn("WebSocket authentication failed: {}", e.getMessage());
        }
    }
}
