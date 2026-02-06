package com.project.gigachat.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {
    
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
}
