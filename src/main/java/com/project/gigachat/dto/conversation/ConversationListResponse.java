package com.project.gigachat.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated list of conversations returned by GET /api/conversations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationListResponse {

    private List<ConversationResponse> conversations;
    private long total;
    private boolean hasMore;
}
