package com.project.gigachat.dto.conversation;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for PATCH /api/conversations/{conversationId} (group chats only).
 */
@Data
public class UpdateConversationRequest {

    @Size(min = 1, max = 100, message = "name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "avatarUrl must be at most 500 characters")
    private String avatarUrl;
}
