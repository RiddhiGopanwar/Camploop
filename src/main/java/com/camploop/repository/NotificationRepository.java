package com.camploop.repository;

import com.camploop.model.Notification;
import com.camploop.model.Profile;
import com.camploop.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop30ByUserOrderByCreatedAtDesc(Profile user);
    long countByUserAndSeenFalse(Profile user);
    List<Notification> findByUserAndSeenFalse(Profile user);
    List<Notification> findByUserAndTypeAndSeenFalse(Profile user, NotificationType type);
    List<Notification> findByUserAndTypeAndReferenceIdAndSeenFalse(Profile user, NotificationType type, Long referenceId);
}
