package com.project.gigachat.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for PATCH /api/messages/{messageId}.
 */
@Data
public class EditMessageRequest {

    @NotBlank(message = "content is required")
    @Size(max = 10000, message = "content is too long")
    private String content;
}
