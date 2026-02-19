package com.project.gigachat.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Single participant info returned in conversation responses and participant list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantResponse {

    private UUID userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private boolean isAdmin;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
}
