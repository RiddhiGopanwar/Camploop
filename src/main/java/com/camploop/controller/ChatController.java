package com.camploop.controller;

import com.camploop.config.AuthContext;
import com.camploop.dto.ConversationResponse;
import com.camploop.dto.MessageResponse;
import com.camploop.dto.SendMessageRequest;
import com.camploop.dto.StartConversationRequest;
import com.camploop.model.Conversation;
import com.camploop.model.Profile;
import com.camploop.service.ChatService;
import com.camploop.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/** Private buyer <-> seller chat. Every endpoint checks the caller is one of the two participants. */
@RestController
@RequestMapping("/api/conversations")
public class ChatController {

    private final ChatService chatService;
    private final ProfileService profileService;
    private final AuthContext authContext;

    @Autowired
    public ChatController(ChatService chatService, ProfileService profileService, AuthContext authContext) {
        this.chatService = chatService;
        this.profileService = profileService;
        this.authContext = authContext;
    }

    /** Open (or create) the chat for a product. */
    @PostMapping
    public ResponseEntity<ConversationResponse> start(@Valid @RequestBody StartConversationRequest body,
                                                      HttpServletRequest http) {
        Profile me = me(http);
        Conversation c = chatService.start(body.getProductId(), body.getBuyerId(), me);
        return ResponseEntity.ok(new ConversationResponse(c, me, chatService.lastMessage(c)));
    }

    /** My inbox: every chat I'm part of, newest activity first. */
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> mine(HttpServletRequest http) {
        Profile me = me(http);
        return ResponseEntity.ok(chatService.list(me).stream()
                .map(c -> new ConversationResponse(c, me, chatService.lastMessage(c)))
                .collect(Collectors.toList()));
    }

    /** Messages in a chat; pass ?after=<lastMessageId> to only get new ones (used for polling). */
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> messages(@PathVariable Long id,
                                                          @RequestParam(required = false, defaultValue = "0") Long after,
                                                          HttpServletRequest http) {
        return ResponseEntity.ok(chatService.messages(id, me(http), after).stream()
                .map(MessageResponse::new)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> send(@PathVariable Long id, @Valid @RequestBody SendMessageRequest body,
                                                HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse(chatService.send(id, me(http), body.getBody())));
    }

    private Profile me(HttpServletRequest http) {
        return profileService.getOrCreate(authContext.require(http), null);
    }
}
