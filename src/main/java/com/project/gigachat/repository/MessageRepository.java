package com.project.gigachat.repository;

import com.project.gigachat.entity.Conversation;
import com.project.gigachat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversation(Conversation conversation, Pageable pageable);

    Page<Message> findByConversationAndCreatedAtBefore(
            Conversation conversation, LocalDateTime before, Pageable pageable);

    Page<Message> findByConversationAndCreatedAtAfter(
            Conversation conversation, LocalDateTime after, Pageable pageable);

    Optional<Message> findFirstByConversationOrderByCreatedAtDesc(Conversation conversation);

    /**
     * Count all unread messages (no lastReadAt — user has never read this conversation).
     */
    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.sender.id != :userId " +
            "AND m.isDeleted = false")
    int countAllUnread(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);

    /**
     * Count messages newer than a known lastReadAt timestamp.
     */
    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.sender.id != :userId " +
            "AND m.isDeleted = false " +
            "AND m.createdAt > :lastReadAt")
    int countUnreadSince(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("lastReadAt") LocalDateTime lastReadAt);

    /**
     * Null-safe helper: delegates to the appropriate query based on whether
     * lastReadAt is known. Avoids passing a null typed parameter to PostgreSQL.
     */
    default int countUnreadMessages(UUID conversationId, UUID userId, LocalDateTime lastReadAt) {
        return lastReadAt == null
                ? countAllUnread(conversationId, userId)
                : countUnreadSince(conversationId, userId, lastReadAt);
    }
}
