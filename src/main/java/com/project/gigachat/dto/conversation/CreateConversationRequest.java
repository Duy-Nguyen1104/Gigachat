package com.project.gigachat.dto.conversation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for POST /api/conversations.
 * Direct: provide type="direct" + participantId
 * Group:  provide type="group"  + participantIds (2+) + name
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotNull(message = "type is required")
    @JsonProperty("type")
    private String type; // "direct" or "group"

    // Direct conversation – exactly one other participant
    @JsonProperty("participantId")
    private UUID participantId;

    // Group conversation – two or more other participants
    @JsonProperty("participantIds")
    private List<UUID> participantIds;

    @Size(max = 100, message = "name must be at most 100 characters")
    @JsonProperty("name")
    private String name;

    @Size(max = 500, message = "avatarUrl must be at most 500 characters")
    @JsonProperty("avatarUrl")
    private String avatarUrl;
}
