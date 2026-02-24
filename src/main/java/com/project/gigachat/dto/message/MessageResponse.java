package com.project.gigachat.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full message details returned by /api/conversations/{id}/messages
 * and by send / edit / reply operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private SenderInfo sender;
    private String content;
    private String type;
    private String attachmentUrl;
    private ReplyInfo replyTo;
    private boolean isEdited;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SenderInfo {
        private UUID id;
        private String username;
        private String displayName;
        private String avatarUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplyInfo {
        private UUID messageId;
        private String content;
        private UUID senderId;
        private String senderName;
    }
}
