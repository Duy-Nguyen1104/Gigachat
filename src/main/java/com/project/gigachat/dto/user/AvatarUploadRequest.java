package com.project.gigachat.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for avatar upload pre-signed URL (POST /api/user/avatar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarUploadRequest {
    
    @NotBlank(message = "File name is required")
    private String fileName;
    
    @NotBlank(message = "File type (MIME) is required")
    private String fileType;
}
