package com.project.gigachat.dto.conversation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for PATCH /api/conversations/{id}/participants/{userId}
 * – promote or demote admin status.
 */
@Data
public class UpdateParticipantRequest {

    @NotNull(message = "isAdmin is required")
    private Boolean isAdmin;
}
