package com.camploop.repository;

import com.camploop.model.Conversation;
import com.camploop.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationAndIdGreaterThanOrderByIdAsc(Conversation conversation, Long afterId);
    Optional<Message> findTopByConversationOrderByIdDesc(Conversation conversation);
}
