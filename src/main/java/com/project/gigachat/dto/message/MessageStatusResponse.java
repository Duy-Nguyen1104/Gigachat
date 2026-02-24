package com.project.gigachat.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Delivery/read receipt info returned by GET /api/messages/{messageId}/status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageStatusResponse {

    private UUID messageId;
    private boolean sent;
    private List<StatusEntry> delivered;
    private List<StatusEntry> read;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusEntry {
        private UUID userId;
        private LocalDateTime timestamp;
    }
}
