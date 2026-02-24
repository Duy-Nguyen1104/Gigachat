package com.project.gigachat.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for POST /api/messages/upload
 * to obtain a pre-signed S3 URL for a message attachment.
 */
@Data
public class UploadUrlRequest {

    @NotBlank(message = "fileName is required")
    @Size(max = 255, message = "fileName is too long")
    private String fileName;

    @NotBlank(message = "fileType is required")
    private String fileType;

    @NotNull(message = "conversationId is required")
    private UUID conversationId;
}
