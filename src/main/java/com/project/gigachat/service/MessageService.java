package com.project.gigachat.service;

import com.project.gigachat.dto.message.*;
import com.project.gigachat.entity.*;
import com.project.gigachat.entity.Message.MessageType;
import com.project.gigachat.entity.MessageStatus.StatusType;
import com.project.gigachat.exception.BadRequestException;
import com.project.gigachat.exception.ResourceNotFoundException;
import com.project.gigachat.exception.UnauthorizedException;
import com.project.gigachat.repository.*;
import com.project.gigachat.service.WebSocketService;
import com.project.gigachat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageStatusRepository statusRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final WebSocketService webSocketService;
    private final SecurityUtils securityUtils;

    @Value("${message.edit.window-minutes:15}")
    private int editWindowMinutes;

    // -------------------------------------------------------------------------
    // GET /api/conversations/{conversationId}/messages
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MessageListResponse getMessages(UUID conversationId, String before, String after, int limit) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);

        PageRequest pageRequest = PageRequest.of(0, limit + 1,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Message> page;
        if (before != null && !before.isBlank()) {
            LocalDateTime cursor = LocalDateTime.parse(before);
            page = messageRepository.findByConversationAndCreatedAtBefore(conv, cursor, pageRequest);
        } else if (after != null && !after.isBlank()) {
            LocalDateTime cursor = LocalDateTime.parse(after);
            page = messageRepository.findByConversationAndCreatedAtAfter(conv, cursor,
                    PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.ASC, "createdAt")));
        } else {
            page = messageRepository.findByConversation(conv, pageRequest);
        }

        List<Message> content = page.getContent();
        boolean hasMore = content.size() > limit;
        List<Message> messages = hasMore ? content.subList(0, limit) : content;

        // Reverse so oldest is first (ascending order for display)
        List<MessageResponse> responses = new java.util.ArrayList<>(
                messages.stream().map(this::toMessageResponse).toList());
        java.util.Collections.reverse(responses);

        String nextCursor = messages.isEmpty() ? null :
                messages.get(messages.size() - 1).getCreatedAt().toString();

        return MessageListResponse.builder()
                .messages(responses)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/conversations/{conversationId}/messages
    // -------------------------------------------------------------------------

    @Transactional
    public MessageResponse sendMessage(UUID conversationId, SendMessageRequest request) {
        User sender = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, sender);

        validateMessageContent(request);

        MessageType type = parseMessageType(request.getType());
        if (type == MessageType.text && (request.getContent() == null || request.getContent().isBlank())) {
            throw new BadRequestException("content is required for text messages");
        }

        Message.MessageBuilder builder = Message.builder()
                .conversation(conv)
                .sender(sender)
                .content(request.getContent())
                .type(type)
                .attachmentUrl(request.getAttachmentUrl());

        if (request.getReplyToMessageId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Reply target message not found: " + request.getReplyToMessageId()));
            builder.replyToMessage(replyTo);
        }

        Message saved = messageRepository.save(builder.build());

        // Create SENT status for sender and mark as read for themselves
        createOrUpdateStatus(saved, sender, StatusType.sent);
        createOrUpdateStatus(saved, sender, StatusType.read);

        // Update lastReadAt in participant record
        participantRepository.findByConversationAndUser(conv, sender)
                .ifPresent(p -> {
                    p.setLastReadAt(LocalDateTime.now());
                    participantRepository.save(p);
                });

        // Bump conversation updatedAt so the sidebar stays ordered
        conversationRepository.save(conv);

        log.info("Message sent in conversation {} by {}", conversationId, sender.getId());
        MessageResponse response = toMessageResponse(saved);
        webSocketService.sendToConversationParticipants(conversationId, "MESSAGE_NEW", response);
        return response;
    }

    // -------------------------------------------------------------------------
    // PATCH /api/messages/{messageId}
    // -------------------------------------------------------------------------

    @Transactional
    public MessageResponse editMessage(UUID messageId, EditMessageRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Message message = findMessageAndVerifyOwner(messageId, currentUser);

        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new BadRequestException("Cannot edit a deleted message");
        }
        if (message.getType() != MessageType.text) {
            throw new BadRequestException("Only text messages can be edited");
        }
        if (message.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(editWindowMinutes))) {
            throw new BadRequestException("Message can only be edited within " + editWindowMinutes + " minutes");
        }

        message.setContent(request.getContent());
        message.setIsEdited(true);
        Message saved = messageRepository.save(message);
        log.info("Message {} edited by {}", messageId, currentUser.getId());
        MessageResponse response = toMessageResponse(saved);
        webSocketService.sendToConversationParticipants(saved.getConversation().getId(), "MESSAGE_UPDATED", response);
        return response;
    }

    // -------------------------------------------------------------------------
    // DELETE /api/messages/{messageId}
    // -------------------------------------------------------------------------

    @Transactional
    public MessageResponse deleteMessage(UUID messageId) {
        User currentUser = securityUtils.getCurrentUser();
        Message message = findMessageAndVerifyOwner(messageId, currentUser);

        message.setIsDeleted(true);
        message.setContent(null);
        Message saved = messageRepository.save(message);
        log.info("Message {} soft-deleted by {}", messageId, currentUser.getId());
        MessageResponse response = toMessageResponse(saved);
        webSocketService.sendToConversationParticipants(saved.getConversation().getId(), "MESSAGE_DELETED", response);
        return response;
    }

    // -------------------------------------------------------------------------
    // POST /api/messages/{messageId}/reply
    // -------------------------------------------------------------------------

    @Transactional
    public MessageResponse replyToMessage(UUID messageId, SendMessageRequest request) {
        User sender = securityUtils.getCurrentUser();
        Message original = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        // Verify participant in the same conversation
        Conversation conv = original.getConversation();
        if (!participantRepository.existsByConversationAndUser(conv, sender)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }

        validateMessageContent(request);
        MessageType type = parseMessageType(request.getType());

        Message reply = Message.builder()
                .conversation(conv)
                .sender(sender)
                .content(request.getContent())
                .type(type)
                .attachmentUrl(request.getAttachmentUrl())
                .replyToMessage(original)
                .build();

        Message saved = messageRepository.save(reply);
        createOrUpdateStatus(saved, sender, StatusType.sent);
        createOrUpdateStatus(saved, sender, StatusType.read);
        conversationRepository.save(conv);
        MessageResponse response = toMessageResponse(saved);
        webSocketService.sendToConversationParticipants(conv.getId(), "MESSAGE_NEW", response);
        return response;
    }

    // -------------------------------------------------------------------------
    // GET /api/messages/{messageId}/status
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MessageStatusResponse getMessageStatus(UUID messageId) {
        User currentUser = securityUtils.getCurrentUser();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        Conversation conv = message.getConversation();
        if (!participantRepository.existsByConversationAndUser(conv, currentUser)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }

        List<MessageStatus> statuses = statusRepository.findByMessage(message);

        List<MessageStatusResponse.StatusEntry> delivered = statuses.stream()
                .filter(s -> s.getStatus() == StatusType.delivered)
                .map(s -> new MessageStatusResponse.StatusEntry(s.getUser().getId(), s.getStatusAt()))
                .toList();

        List<MessageStatusResponse.StatusEntry> read = statuses.stream()
                .filter(s -> s.getStatus() == StatusType.read)
                .map(s -> new MessageStatusResponse.StatusEntry(s.getUser().getId(), s.getStatusAt()))
                .toList();

        return MessageStatusResponse.builder()
                .messageId(messageId)
                .sent(true)
                .delivered(delivered)
                .read(read)
                .build();
    }

    // -------------------------------------------------------------------------
    // PATCH /api/messages/{messageId}/status
    // -------------------------------------------------------------------------

    @Transactional
    public void updateMessageStatus(UUID messageId, String statusStr) {
        User currentUser = securityUtils.getCurrentUser();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        Conversation conv = message.getConversation();
        if (!participantRepository.existsByConversationAndUser(conv, currentUser)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }

        StatusType status;
        try {
            status = StatusType.valueOf(statusStr.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + statusStr);
        }

        createOrUpdateStatus(message, currentUser, status);
    }

    // -------------------------------------------------------------------------
    // POST /api/messages/upload
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public UploadUrlResponse generateUploadUrl(UploadUrlRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(request.getConversationId(), currentUser);

        S3Service.PresignedUploadResult result = s3Service.generateMessageUploadUrl(
                conv.getId(), currentUser.getId(), request.getFileName(), request.getFileType());

        return UploadUrlResponse.builder()
                .uploadUrl(result.uploadUrl())
                .fileKey(result.key())
                .expiresIn(result.expiresInMinutes())
                .build();
    }

    // -------------------------------------------------------------------------
    // Package-visible helper used by ConversationService
    // -------------------------------------------------------------------------

    /**
     * Build the last-message summary for a conversation, used in sidebar listing.
     */
    public MessageResponse.ReplyInfo getLastMessageSummary(Conversation conv) {
        return messageRepository.findFirstByConversationOrderByCreatedAtDesc(conv)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(m -> MessageResponse.ReplyInfo.builder()
                        .messageId(m.getId())
                        .content(m.getContent())
                        .senderId(m.getSender().getId())
                        .senderName(m.getSender().getDisplayName() != null
                                ? m.getSender().getDisplayName()
                                : m.getSender().getUsername())
                        .build())
                .orElse(null);
    }

    /**
     * Count unread messages for a user in a conversation.
     */
    public int countUnread(Conversation conv, ConversationParticipant participant) {
        return messageRepository.countUnreadMessages(
                conv.getId(),
                participant.getUser().getId(),
                participant.getLastReadAt());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Conversation findConversationAndVerifyParticipant(UUID conversationId, User user) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        if (!participantRepository.existsByConversationAndUser(conv, user)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }
        return conv;
    }

    private Message findMessageAndVerifyOwner(UUID messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        if (!message.getSender().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not the owner of this message");
        }
        return message;
    }

    private MessageType parseMessageType(String type) {
        try {
            return MessageType.valueOf(type.toLowerCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Invalid message type: " + type + ". Allowed: text, image, file, voice");
        }
    }

    private void validateMessageContent(SendMessageRequest request) {
        String type = request.getType();
        if ("image".equals(type) || "file".equals(type) || "voice".equals(type)) {
            if (request.getAttachmentUrl() == null || request.getAttachmentUrl().isBlank()) {
                throw new BadRequestException("attachmentUrl is required for " + type + " messages");
            }
        }
    }

    private void createOrUpdateStatus(Message message, User user, StatusType status) {
        statusRepository.findByMessageAndUser(message, user)
                .ifPresentOrElse(
                        existing -> {
                            existing.setStatus(status);
                            existing.setStatusAt(LocalDateTime.now());
                            statusRepository.save(existing);
                        },
                        () -> statusRepository.save(MessageStatus.builder()
                                .message(message)
                                .user(user)
                                .status(status)
                                .statusAt(LocalDateTime.now())
                                .build())
                );
    }

    public MessageResponse toMessageResponse(Message message) {
        MessageResponse.SenderInfo senderInfo = MessageResponse.SenderInfo.builder()
                .id(message.getSender().getId())
                .username(message.getSender().getUsername())
                .displayName(message.getSender().getDisplayName())
                .avatarUrl(s3Service.generatePresignedGetUrl(message.getSender().getAvatarUrl()))
                .build();

        MessageResponse.ReplyInfo replyInfo = null;
        if (message.getReplyToMessage() != null) {
            Message reply = message.getReplyToMessage();
            replyInfo = MessageResponse.ReplyInfo.builder()
                    .messageId(reply.getId())
                    .content(reply.getContent())
                    .senderId(reply.getSender().getId())
                    .senderName(reply.getSender().getDisplayName() != null
                            ? reply.getSender().getDisplayName()
                            : reply.getSender().getUsername())
                    .build();
        }

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .sender(senderInfo)
                .content(Boolean.TRUE.equals(message.getIsDeleted()) ? null : message.getContent())
                .type(message.getType().name())
                .attachmentUrl(s3Service.generatePresignedGetUrl(message.getAttachmentUrl()))
                .replyTo(replyInfo)
                .isEdited(Boolean.TRUE.equals(message.getIsEdited()))
                .isDeleted(Boolean.TRUE.equals(message.getIsDeleted()))
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
