package com.project.gigachat.repository;

import com.project.gigachat.entity.Conversation;
import com.project.gigachat.entity.ConversationParticipant;
import com.project.gigachat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    List<ConversationParticipant> findByConversation(Conversation conversation);

    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);

    boolean existsByConversationAndUser(Conversation conversation, User user);

    void deleteByConversationAndUser(Conversation conversation, User user);

    long countByConversation(Conversation conversation);

    List<ConversationParticipant> findByConversationAndIsAdmin(Conversation conversation, Boolean isAdmin);

    /** All conversations a given user participates in — used for presence broadcasting. */
    List<ConversationParticipant> findByUser(User user);
}
