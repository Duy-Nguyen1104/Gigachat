package com.project.gigachat.service;

import com.project.gigachat.service.S3Service;
import com.project.gigachat.dto.conversation.*;
import com.project.gigachat.entity.Conversation;
import com.project.gigachat.entity.Conversation.ConversationType;
import com.project.gigachat.entity.ConversationParticipant;
import com.project.gigachat.entity.Message;
import com.project.gigachat.entity.User;
import com.project.gigachat.exception.BadRequestException;
import com.project.gigachat.exception.ConflictException;
import com.project.gigachat.exception.ResourceNotFoundException;
import com.project.gigachat.exception.UnauthorizedException;
import com.project.gigachat.repository.ConversationParticipantRepository;
import com.project.gigachat.repository.ConversationRepository;
import com.project.gigachat.repository.MessageRepository;
import com.project.gigachat.repository.UserRepository;
import com.project.gigachat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SecurityUtils securityUtils;
    private final S3Service s3Service;

    // -------------------------------------------------------------------------
    // GET /api/conversations
    // -------------------------------------------------------------------------

    /**
     * Return paginated list of conversations the current user belongs to,
     * ordered by most recently updated first.
     */
    @Transactional(readOnly = true)
    public ConversationListResponse getConversations(int page, int size) {
        User currentUser = securityUtils.getCurrentUser();
        Page<Conversation> convPage = conversationRepository.findByParticipantsUser(
                currentUser, PageRequest.of(page, size));

        List<ConversationResponse> responses = convPage.getContent().stream()
                .map(c -> toResponse(c, currentUser))
                .toList();

        return ConversationListResponse.builder()
                .conversations(responses)
                .total(convPage.getTotalElements())
                .hasMore(convPage.hasNext())
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/conversations
    // -------------------------------------------------------------------------

    /**
     * Create a new direct or group conversation.
     * For direct: finds or creates the conversation between two users.
     * For group: requires at least 2 other participants and a name.
     */
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        User creator = securityUtils.getCurrentUser();

        if ("direct".equalsIgnoreCase(request.getType())) {
            return createDirectConversation(creator, request);
        } else if ("group".equalsIgnoreCase(request.getType())) {
            return createGroupConversation(creator, request);
        } else {
            throw new BadRequestException("type must be 'direct' or 'group'");
        }
    }

    private ConversationResponse createDirectConversation(User creator, CreateConversationRequest request) {
        UUID otherId = request.getParticipantId();
        if (otherId == null) {
            throw new BadRequestException("participantId is required for direct conversations");
        }
        if (otherId.equals(creator.getId())) {
            throw new BadRequestException("Cannot start a conversation with yourself");
        }

        User other = userRepository.findById(otherId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + otherId));

        // Return existing conversation if one already exists
        return conversationRepository.findDirectConversationBetweenUsers(creator.getId(), other.getId())
                .map(existing -> toResponse(existing, creator))
                .orElseGet(() -> {
                    Conversation conv = Conversation.builder()
                            .type(ConversationType.direct)
                            .createdBy(creator)
                            .build();
                    Conversation saved = conversationRepository.save(conv);

                    addParticipantInternal(saved, creator, true);
                    addParticipantInternal(saved, other, false);

                    log.info("Direct conversation created: {} between {} and {}", saved.getId(), creator.getId(), other.getId());
                    return toResponse(saved, creator);
                });
    }

    private ConversationResponse createGroupConversation(User creator, CreateConversationRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("name is required for group conversations");
        }
        if (request.getParticipantIds() == null || request.getParticipantIds().size() < 2) {
            throw new BadRequestException("Group conversations require at least 2 other participants");
        }

        List<User> others = new ArrayList<>();
        for (UUID uid : request.getParticipantIds()) {
            if (uid.equals(creator.getId())) continue; // skip self if accidentally included
            User u = userRepository.findById(uid)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uid));
            others.add(u);
        }
        if (others.size() < 2) {
            throw new BadRequestException("Group conversations require at least 2 other participants");
        }

        Conversation conv = Conversation.builder()
                .type(ConversationType.group)
                .name(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .createdBy(creator)
                .build();
        Conversation saved = conversationRepository.save(conv);

        addParticipantInternal(saved, creator, true); // creator is admin
        for (User other : others) {
            addParticipantInternal(saved, other, false);
        }

        log.info("Group conversation created: {} by {}", saved.getId(), creator.getId());
        return toResponse(saved, creator);
    }

    // -------------------------------------------------------------------------
    // GET /api/conversations/{conversationId}
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ConversationResponse getConversationDetails(UUID conversationId) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);
        return toResponse(conv, currentUser);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/conversations/{conversationId}
    // -------------------------------------------------------------------------

    @Transactional
    public ConversationResponse updateConversation(UUID conversationId, UpdateConversationRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);

        if (conv.getType() == ConversationType.direct) {
            throw new BadRequestException("Direct conversations cannot be renamed");
        }
        verifyAdmin(conv, currentUser);

        if (request.getName() != null) {
            conv.setName(request.getName());
        }
        if (request.getAvatarUrl() != null) {
            conv.setAvatarUrl(request.getAvatarUrl());
        }

        Conversation saved = conversationRepository.save(conv);
        return toResponse(saved, currentUser);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/conversations/{conversationId} — leave conversation
    // -------------------------------------------------------------------------

    @Transactional
    public void leaveConversation(UUID conversationId) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);

        participantRepository.deleteByConversationAndUser(conv, currentUser);

        long remaining = participantRepository.countByConversation(conv);
        if (remaining == 0) {
            conversationRepository.delete(conv);
            log.info("Conversation {} deleted (no participants remaining)", conversationId);
        } else {
            log.info("User {} left conversation {}", currentUser.getId(), conversationId);
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/conversations/{conversationId}/participants
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(UUID conversationId) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);
        return participantRepository.findByConversation(conv).stream()
                .map(this::toParticipantResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // POST /api/conversations/{conversationId}/participants
    // -------------------------------------------------------------------------

    @Transactional
    public ParticipantResponse addParticipant(UUID conversationId, AddParticipantRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);

        if (conv.getType() == ConversationType.direct) {
            throw new BadRequestException("Cannot add participants to a direct conversation");
        }
        verifyAdmin(conv, currentUser);

        User newUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        if (participantRepository.existsByConversationAndUser(conv, newUser)) {
            throw new ConflictException("User is already a participant");
        }

        ConversationParticipant participant = addParticipantInternal(conv, newUser, false);
        return toParticipantResponse(participant);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/conversations/{conversationId}/participants/{userId}
    // -------------------------------------------------------------------------

    @Transactional
    public void removeParticipant(UUID conversationId, UUID userId) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);

        if (conv.getType() == ConversationType.direct) {
            throw new BadRequestException("Cannot remove participants from a direct conversation");
        }
        verifyAdmin(conv, currentUser);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        ConversationParticipant target = participantRepository.findByConversationAndUser(conv, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a participant"));

        // Prevent removing the last admin
        if (Boolean.TRUE.equals(target.getIsAdmin())) {
            long adminCount = participantRepository.findByConversationAndIsAdmin(conv, true).size();
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot remove the last admin from the conversation");
            }
        }

        participantRepository.deleteByConversationAndUser(conv, targetUser);
        log.info("User {} removed from conversation {} by {}", userId, conversationId, currentUser.getId());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/conversations/{conversationId}/participants/{userId}
    // -------------------------------------------------------------------------

    @Transactional
    public ParticipantResponse updateParticipant(UUID conversationId, UUID userId, UpdateParticipantRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);

        if (conv.getType() == ConversationType.direct) {
            throw new BadRequestException("Cannot update participant roles in a direct conversation");
        }
        verifyAdmin(conv, currentUser);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        ConversationParticipant participant = participantRepository.findByConversationAndUser(conv, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("User is not a participant"));

        // Prevent demoting the last admin
        if (Boolean.FALSE.equals(request.getIsAdmin()) && Boolean.TRUE.equals(participant.getIsAdmin())) {
            long adminCount = participantRepository.findByConversationAndIsAdmin(conv, true).size();
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot demote the last admin");
            }
        }

        participant.setIsAdmin(request.getIsAdmin());
        ConversationParticipant saved = participantRepository.save(participant);
        return toParticipantResponse(saved);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/conversations/{conversationId}/mute  &  /unmute
    // -------------------------------------------------------------------------

    @Transactional
    public void muteConversation(UUID conversationId) {
        setMuted(conversationId, true);
    }

    @Transactional
    public void unmuteConversation(UUID conversationId) {
        setMuted(conversationId, false);
    }

    private void setMuted(UUID conversationId, boolean muted) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);
        ConversationParticipant participant = participantRepository.findByConversationAndUser(conv, currentUser)
                .orElseThrow(() -> new UnauthorizedException("Not a participant"));
        participant.setIsMuted(muted);
        participantRepository.save(participant);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/conversations/{conversationId}/read
    // -------------------------------------------------------------------------

    @Transactional
    public void markAsRead(UUID conversationId) {
        User currentUser = securityUtils.getCurrentUser();
        Conversation conv = findConversationAndVerifyParticipant(conversationId, currentUser);
        ConversationParticipant participant = participantRepository.findByConversationAndUser(conv, currentUser)
                .orElseThrow(() -> new UnauthorizedException("Not a participant"));
        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Conversation findConversationAndVerifyParticipant(UUID conversationId, User user) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        if (!participantRepository.existsByConversationAndUser(conv, user)) {
            throw new UnauthorizedException("You are not a participant in this conversation");
        }
        return conv;
    }

    private void verifyAdmin(Conversation conv, User user) {
        ConversationParticipant participant = participantRepository.findByConversationAndUser(conv, user)
                .orElseThrow(() -> new UnauthorizedException("Not a participant"));
        if (!Boolean.TRUE.equals(participant.getIsAdmin())) {
            throw new UnauthorizedException("Admin privileges required");
        }
    }

    private ConversationParticipant addParticipantInternal(Conversation conv, User user, boolean isAdmin) {
        ConversationParticipant p = ConversationParticipant.builder()
                .conversation(conv)
                .user(user)
                .isAdmin(isAdmin)
                .build();
        return participantRepository.save(p);
    }

    private ConversationResponse toResponse(Conversation conv, User currentUser) {
        List<ConversationParticipant> participants = participantRepository.findByConversation(conv);

        ConversationParticipant currentParticipant = participants.stream()
                .filter(p -> p.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);

        boolean muted = currentParticipant != null && Boolean.TRUE.equals(currentParticipant.getIsMuted());

        // Populate last message
        ConversationResponse.LastMessageInfo lastMessageInfo = messageRepository
                .findFirstByConversationOrderByCreatedAtDesc(conv)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(m -> ConversationResponse.LastMessageInfo.builder()
                        .id(m.getId())
                        .content(m.getContent())
                        .senderId(m.getSender().getId())
                        .senderName(m.getSender().getDisplayName() != null
                                ? m.getSender().getDisplayName()
                                : m.getSender().getUsername())
                        .createdAt(m.getCreatedAt())
                        .type(m.getType().name())
                        .build())
                .orElse(null);

        // Count unread messages for the current user
        int unreadCount = currentParticipant != null
                ? messageRepository.countUnreadMessages(
                        conv.getId(),
                        currentUser.getId(),
                        currentParticipant.getLastReadAt())
                : 0;

        // For direct conversations derive display name from the other participant
        String displayName = conv.getName();
        String avatarUrl = conv.getAvatarUrl();
        if (conv.getType() == ConversationType.direct && displayName == null) {
            displayName = participants.stream()
                    .filter(p -> !p.getUser().getId().equals(currentUser.getId()))
                    .findFirst()
                    .map(p -> p.getUser().getDisplayName() != null
                            ? p.getUser().getDisplayName()
                            : p.getUser().getUsername())
                    .orElse(null);
            if (avatarUrl == null) {
                avatarUrl = participants.stream()
                        .filter(p -> !p.getUser().getId().equals(currentUser.getId()))
                        .findFirst()
                        .map(p -> p.getUser().getAvatarUrl())
                        .orElse(null);
            }
        }

        return ConversationResponse.builder()
                .id(conv.getId())
                .type(conv.getType().name())
                .name(displayName)
                .avatarUrl(s3Service.generatePresignedGetUrl(avatarUrl))
                .createdBy(conv.getCreatedBy().getId())
                .participants(participants.stream().map(this::toParticipantResponse).toList())
                .lastMessage(lastMessageInfo)
                .unreadCount(unreadCount)
                .isMuted(muted)
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    private ParticipantResponse toParticipantResponse(ConversationParticipant p) {
        return ParticipantResponse.builder()
                .userId(p.getUser().getId())
                .username(p.getUser().getUsername())
                .displayName(p.getUser().getDisplayName())
                .avatarUrl(s3Service.generatePresignedGetUrl(p.getUser().getAvatarUrl()))
                .isAdmin(Boolean.TRUE.equals(p.getIsAdmin()))
                .joinedAt(p.getJoinedAt())
                .lastReadAt(p.getLastReadAt())
                .build();
    }
}
