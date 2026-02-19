package com.project.gigachat.repository;

import com.project.gigachat.entity.Conversation;
import com.project.gigachat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Find all conversations where the given user is a participant,
     * ordered by the most recently updated conversation first.
     */
    @Query("SELECT c FROM Conversation c " +
            "JOIN c.participants p " +
            "WHERE p.user = :user " +
            "ORDER BY c.updatedAt DESC")
    Page<Conversation> findByParticipantsUser(@Param("user") User user, Pageable pageable);

    /**
     * Find an existing direct conversation between exactly two users.
     * Returns the conversation only if both users are participants.
     */
    @Query("SELECT c FROM Conversation c " +
            "JOIN c.participants p1 ON p1.user.id = :userA " +
            "JOIN c.participants p2 ON p2.user.id = :userB " +
            "WHERE c.type = 'direct'")
    Optional<Conversation> findDirectConversationBetweenUsers(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB);
}
