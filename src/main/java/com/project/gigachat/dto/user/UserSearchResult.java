package com.project.gigachat.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Minimal user info returned in search results (GET /api/users/search).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResult {
    
    private UUID id;
    private String username;
    private String displayName;
    private String avatarUrl;
}
