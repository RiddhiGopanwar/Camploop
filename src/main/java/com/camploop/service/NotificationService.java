package com.camploop.service;

import com.camploop.model.Notification;
import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.enums.NotificationType;
import com.camploop.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(Profile recipient, NotificationType type, String message, Product product, Long referenceId) {
        notificationRepository.save(new Notification(recipient, type, message, product, referenceId));
    }

    /** One unread "new message" notification per conversation — don't spam on every chat line. */
    public void notifyNewMessageOnce(Profile recipient, String message, Product product, Long conversationId) {
        boolean alreadyUnread = !notificationRepository
                .findByUserAndTypeAndReferenceIdAndSeenFalse(recipient, NotificationType.NEW_MESSAGE, conversationId)
                .isEmpty();
        if (!alreadyUnread) {
            notify(recipient, NotificationType.NEW_MESSAGE, message, product, conversationId);
        }
    }

    public List<Notification> latest(Profile user) {
        return notificationRepository.findTop30ByUserOrderByCreatedAtDesc(user);
    }

    public long unreadCount(Profile user) {
        return notificationRepository.countByUserAndSeenFalse(user);
    }

    /** type == null marks everything read. */
    public void markRead(Profile user, NotificationType type) {
        List<Notification> unread = (type == null)
                ? notificationRepository.findByUserAndSeenFalse(user)
                : notificationRepository.findByUserAndTypeAndSeenFalse(user, type);
        unread.forEach(n -> n.setSeen(true));
        notificationRepository.saveAll(unread);
    }

    public void markConversationRead(Profile user, Long conversationId) {
        List<Notification> unread = notificationRepository
                .findByUserAndTypeAndReferenceIdAndSeenFalse(user, NotificationType.NEW_MESSAGE, conversationId);
        unread.forEach(n -> n.setSeen(true));
        notificationRepository.saveAll(unread);
    }
}
