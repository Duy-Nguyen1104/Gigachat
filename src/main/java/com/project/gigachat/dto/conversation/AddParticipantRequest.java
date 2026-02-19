package com.project.gigachat.dto.conversation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for POST /api/conversations/{id}/participants.
 */
@Data
public class AddParticipantRequest {

    @NotNull(message = "userId is required")
    private UUID userId;
}
