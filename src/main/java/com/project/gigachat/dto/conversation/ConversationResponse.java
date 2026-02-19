package com.project.gigachat.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full conversation details returned by GET /api/conversations/{id}
 * and by POST /api/conversations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {

    private UUID id;
    private String type;
    private String name;
    private String avatarUrl;
    private UUID createdBy;
    private List<ParticipantResponse> participants;
    private LastMessageInfo lastMessage;
    private int unreadCount;
    private boolean isMuted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LastMessageInfo {
        private UUID id;
        private String content;
        private UUID senderId;
        private String senderName;
        private LocalDateTime createdAt;
        private String type;
    }
}
