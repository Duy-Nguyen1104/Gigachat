package com.project.gigachat.repository;

import com.project.gigachat.entity.Message;
import com.project.gigachat.entity.MessageStatus;
import com.project.gigachat.entity.MessageStatus.StatusType;
import com.project.gigachat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, UUID> {

    List<MessageStatus> findByMessage(Message message);

    Optional<MessageStatus> findByMessageAndUser(Message message, User user);

    List<MessageStatus> findByMessageAndStatus(Message message, StatusType status);
}
