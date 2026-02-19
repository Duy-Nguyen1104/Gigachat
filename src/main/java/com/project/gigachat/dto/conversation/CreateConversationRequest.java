package com.project.gigachat.dto.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Request body for POST /api/conversations.
 * Direct: provide type="direct" + participantId
 * Group:  provide type="group"  + participantIds (2+) + name
 */
@Data
public class CreateConversationRequest {

    @NotNull(message = "type is required")
    private String type; // "direct" or "group"

    // Direct conversation – exactly one other participant
    private UUID participantId;

    // Group conversation – two or more other participants
    private List<UUID> participantIds;

    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @Size(max = 500, message = "avatarUrl must be at most 500 characters")
    private String avatarUrl;
}
