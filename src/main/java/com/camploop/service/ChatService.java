package com.camploop.service;

import com.camploop.exception.ApiException;
import com.camploop.model.Conversation;
import com.camploop.model.Message;
import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.enums.ListingStatus;
import com.camploop.repository.ConversationRepository;
import com.camploop.repository.MessageRepository;
import com.camploop.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final ProductService productService;
    private final PurchaseRequestService purchaseRequestService;
    private final NotificationService notificationService;

    @Autowired
    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       ProfileRepository profileRepository,
                       ProductService productService,
                       PurchaseRequestService purchaseRequestService,
                       NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.profileRepository = profileRepository;
        this.productService = productService;
        this.purchaseRequestService = purchaseRequestService;
        this.notificationService = notificationService;
    }

    /**
     * Gets or creates the private chat for (product, buyer).
     *  - A buyer opens it for themselves ("Contact Seller").
     *  - The seller may only open it with a buyer who has already sent a request or chat.
     */
    @Transactional
    public Conversation start(Long productId, UUID buyerId, Profile caller) {
        Product product = productService.getById(productId);
        Profile seller = product.getSeller();
        boolean callerIsSeller = seller.getId().equals(caller.getId());

        Profile buyer;
        if (callerIsSeller) {
            if (buyerId == null) {
                throw new ApiException("Choose which buyer to chat with", HttpStatus.BAD_REQUEST);
            }
            buyer = profileRepository.findById(buyerId)
                    .orElseThrow(() -> new ApiException("Buyer not found", HttpStatus.NOT_FOUND));
            boolean known = conversationRepository.findByProductAndBuyer(product, buyer).isPresent()
                    || purchaseRequestService.hasRequest(product, buyer);
            if (!known) {
                throw new ApiException("That student hasn't contacted you about this item", HttpStatus.FORBIDDEN);
            }
        } else {
            buyer = caller;
        }

        return conversationRepository.findByProductAndBuyer(product, buyer).orElseGet(() -> {
            if (callerIsSeller) {
                throw new ApiException("No conversation exists with this buyer yet", HttpStatus.NOT_FOUND);
            }
            if (product.getStatus() != ListingStatus.AVAILABLE) {
                throw new ApiException("This item is no longer available", HttpStatus.CONFLICT);
            }
            Conversation c = new Conversation();
            c.setProduct(product);
            c.setBuyer(buyer);
            c.setSeller(seller);
            return conversationRepository.save(c);
        });
    }

    public List<Conversation> list(Profile user) {
        List<Conversation> all = conversationRepository.findForUser(user);
        all.sort(Comparator.comparing((Conversation c) -> lastActivity(c)).reversed());
        return all;
    }

    public Message lastMessage(Conversation c) {
        return messageRepository.findTopByConversationOrderByIdDesc(c).orElse(null);
    }

    /** Messages newer than {@code afterId} (0 = all). Participants only. */
    @Transactional
    public List<Message> messages(Long conversationId, Profile user, Long afterId) {
        Conversation c = requireParticipant(conversationId, user);
        notificationService.markConversationRead(user, c.getId());
        return messageRepository.findByConversationAndIdGreaterThanOrderByIdAsc(c, afterId == null ? 0L : afterId);
    }

    @Transactional
    public Message send(Long conversationId, Profile sender, String body) {
        Conversation c = requireParticipant(conversationId, sender);
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw new ApiException("Message can't be empty", HttpStatus.BAD_REQUEST);
        }

        Message m = new Message();
        m.setConversation(c);
        m.setSender(sender);
        m.setBody(text);
        m = messageRepository.save(m);

        Profile recipient = c.otherParty(sender);
        notificationService.notifyNewMessageOnce(recipient,
                sender.getName() + " sent you a message about \"" + c.getProduct().getName() + "\"",
                c.getProduct(), c.getId());
        return m;
    }

    public Conversation requireParticipant(Long conversationId, Profile user) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException("Conversation not found", HttpStatus.NOT_FOUND));
        if (!c.hasParticipant(user)) {
            // 404 rather than 403 so outsiders can't even confirm a conversation exists
            throw new ApiException("Conversation not found", HttpStatus.NOT_FOUND);
        }
        return c;
    }

    private java.time.LocalDateTime lastActivity(Conversation c) {
        Message last = lastMessage(c);
        return last != null ? last.getCreatedAt() : c.getCreatedAt();
    }
}
