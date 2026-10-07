package com.camploop.repository;

import com.camploop.model.Conversation;
import com.camploop.model.Product;
import com.camploop.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByProductAndBuyer(Product product, Profile buyer);

    @Query("SELECT c FROM Conversation c WHERE c.buyer = :user OR c.seller = :user")
    List<Conversation> findForUser(@Param("user") Profile user);
}
