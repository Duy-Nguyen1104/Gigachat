package com.project.gigachat.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO returned after a profile update (PATCH /api/user/me).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileResponse {
    
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime updatedAt;
}
