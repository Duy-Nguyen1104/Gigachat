package com.project.gigachat.controller;

import com.project.gigachat.dto.message.*;
import com.project.gigachat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * GET /api/conversations/{conversationId}/messages
     * Cursor-based pagination. Use `before` (ISO timestamp) to load older messages.
     */
    @GetMapping("/api/conversations/{conversationId}/messages")
    public ResponseEntity<MessageListResponse> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(messageService.getMessages(conversationId, before, after, limit));
    }

    /**
     * POST /api/conversations/{conversationId}/messages
     * Send a new message in a conversation.
     */
    @PostMapping("/api/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(conversationId, request));
    }

    /**
     * POST /api/messages/{messageId}/reply
     * Reply to an existing message.
     */
    @PostMapping("/api/messages/{messageId}/reply")
    public ResponseEntity<MessageResponse> replyToMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.replyToMessage(messageId, request));
    }

    /**
     * PATCH /api/messages/{messageId}
     * Edit the content of an owned text message (within edit window).
     */
    @PatchMapping("/api/messages/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(messageService.editMessage(messageId, request));
    }

    /**
     * DELETE /api/messages/{messageId}
     * Soft-delete an owned message.
     */
    @DeleteMapping("/api/messages/{messageId}")
    public ResponseEntity<MessageResponse> deleteMessage(@PathVariable UUID messageId) {
        return ResponseEntity.ok(messageService.deleteMessage(messageId));
    }

    /**
     * GET /api/messages/{messageId}/status
     * Get delivery/read receipts for a message (must be a conversation participant).
     */
    @GetMapping("/api/messages/{messageId}/status")
    public ResponseEntity<MessageStatusResponse> getMessageStatus(@PathVariable UUID messageId) {
        return ResponseEntity.ok(messageService.getMessageStatus(messageId));
    }

    /**
     * PATCH /api/messages/{messageId}/status
     * Update your own delivery/read status for a message.
     * Body: { "status": "delivered" } or { "status": "read" }
     */
    @PatchMapping("/api/messages/{messageId}/status")
    public ResponseEntity<Void> updateMessageStatus(
            @PathVariable UUID messageId,
            @RequestBody java.util.Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        messageService.updateMessageStatus(messageId, status);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/messages/upload
     * Generate a pre-signed S3 upload URL for a message attachment.
     */
    @PostMapping("/api/messages/upload")
    public ResponseEntity<UploadUrlResponse> generateUploadUrl(
            @Valid @RequestBody UploadUrlRequest request) {
        return ResponseEntity.ok(messageService.generateUploadUrl(request));
    }
}
