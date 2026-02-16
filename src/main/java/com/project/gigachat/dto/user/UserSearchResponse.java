package com.project.gigachat.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated search response (GET /api/users/search).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResponse {
    
    private List<UserSearchResult> users;
    private boolean hasMore;
}
