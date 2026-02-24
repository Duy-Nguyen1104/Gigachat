package com.project.gigachat.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated list of messages returned by GET /api/conversations/{id}/messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageListResponse {

    private List<MessageResponse> messages;
    private boolean hasMore;
    private String nextCursor; // ISO timestamp of the oldest message in this page
}
