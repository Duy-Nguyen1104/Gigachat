package com.project.gigachat.dto.message;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for POST /api/conversations/{conversationId}/messages
 * and for POST /api/messages/{messageId}/reply.
 */
@Data
public class SendMessageRequest {

    @NotNull(message = "type is required")
    private String type; // "text", "image", "file", "voice"

    @Size(max = 10000, message = "content is too long")
    private String content;

    @Size(max = 500, message = "attachmentUrl is too long")
    private String attachmentUrl;

    private UUID replyToMessageId;
}
