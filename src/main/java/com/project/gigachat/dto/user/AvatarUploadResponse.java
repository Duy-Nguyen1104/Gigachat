package com.project.gigachat.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for avatar upload pre-signed URL (POST /api/user/avatar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarUploadResponse {
    
    private String uploadUrl;
    private String fileUrl;
    private int expiresInMinutes;
}
