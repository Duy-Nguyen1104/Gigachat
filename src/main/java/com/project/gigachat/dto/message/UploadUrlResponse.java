package com.project.gigachat.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /api/messages/upload — pre-signed S3 upload URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadUrlResponse {

    private String uploadUrl;
    private String fileKey;
    private int expiresIn; // minutes
}
