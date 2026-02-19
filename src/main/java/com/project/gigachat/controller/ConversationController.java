package com.project.gigachat.controller;

import com.project.gigachat.dto.conversation.*;
import com.project.gigachat.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conversation management endpoints.
 *
 * Base:        /api/conversations
 * Participants:/api/conversations/{conversationId}/participants
 * Settings:    /api/conversations/{conversationId}/mute | unmute | read
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    // -------------------------------------------------------------------------
    // Conversation CRUD
    // -------------------------------------------------------------------------

    /**
     * GET /api/conversations?page=0&size=20
     * Returns all conversations the authenticated user participates in.
     */
    @GetMapping
    public ResponseEntity<ConversationListResponse> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(conversationService.getConversations(page, size));
    }

    /**
     * POST /api/conversations
     * Create a new direct or group conversation.
     */
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse response = conversationService.createConversation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/conversations/{conversationId}
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversationDetails(
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationService.getConversationDetails(conversationId));
    }

    /**
     * PATCH /api/conversations/{conversationId}
     * Update name/avatarUrl – group chats only, admin required.
     */
    @PatchMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> updateConversation(
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateConversationRequest request) {
        return ResponseEntity.ok(conversationService.updateConversation(conversationId, request));
    }

    /**
     * DELETE /api/conversations/{conversationId}
     * Removes the current user from the conversation. Deletes it if last participant.
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Map<String, String>> leaveConversation(
            @PathVariable UUID conversationId) {
        conversationService.leaveConversation(conversationId);
        return ResponseEntity.ok(Map.of("message", "Left conversation"));
    }

    // -------------------------------------------------------------------------
    // Participants
    // -------------------------------------------------------------------------

    /**
     * GET /api/conversations/{conversationId}/participants
     */
    @GetMapping("/{conversationId}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationService.getParticipants(conversationId));
    }

    /**
     * POST /api/conversations/{conversationId}/participants
     * Add a user to a group conversation (admin required).
     */
    @PostMapping("/{conversationId}/participants")
    public ResponseEntity<ParticipantResponse> addParticipant(
            @PathVariable UUID conversationId,
            @Valid @RequestBody AddParticipantRequest request) {
        ParticipantResponse response = conversationService.addParticipant(conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/conversations/{conversationId}/participants/{userId}
     * Remove a participant (admin required).
     */
    @DeleteMapping("/{conversationId}/participants/{userId}")
    public ResponseEntity<Map<String, String>> removeParticipant(
            @PathVariable UUID conversationId,
            @PathVariable UUID userId) {
        conversationService.removeParticipant(conversationId, userId);
        return ResponseEntity.ok(Map.of("message", "Participant removed"));
    }

    /**
     * PATCH /api/conversations/{conversationId}/participants/{userId}
     * Promote or demote admin status (admin required).
     */
    @PatchMapping("/{conversationId}/participants/{userId}")
    public ResponseEntity<ParticipantResponse> updateParticipant(
            @PathVariable UUID conversationId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateParticipantRequest request) {
        return ResponseEntity.ok(conversationService.updateParticipant(conversationId, userId, request));
    }

    // -------------------------------------------------------------------------
    // Settings
    // -------------------------------------------------------------------------

    /**
     * PATCH /api/conversations/{conversationId}/mute
     */
    @PatchMapping("/{conversationId}/mute")
    public ResponseEntity<Map<String, Object>> muteConversation(
            @PathVariable UUID conversationId) {
        conversationService.muteConversation(conversationId);
        return ResponseEntity.ok(Map.of("conversationId", conversationId, "isMuted", true));
    }

    /**
     * PATCH /api/conversations/{conversationId}/unmute
     */
    @PatchMapping("/{conversationId}/unmute")
    public ResponseEntity<Map<String, Object>> unmuteConversation(
            @PathVariable UUID conversationId) {
        conversationService.unmuteConversation(conversationId);
        return ResponseEntity.ok(Map.of("conversationId", conversationId, "isMuted", false));
    }

    /**
     * PATCH /api/conversations/{conversationId}/read
     * Updates lastReadAt for the current user in this conversation.
     */
    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable UUID conversationId) {
        conversationService.markAsRead(conversationId);
        return ResponseEntity.ok(Map.of("conversationId", conversationId, "marked", "read"));
    }
}
