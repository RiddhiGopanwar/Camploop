package com.camploop.controller;

import com.camploop.config.AuthContext;
import com.camploop.dto.ApiMessageResponse;
import com.camploop.dto.NotificationResponse;
import com.camploop.model.Profile;
import com.camploop.model.enums.NotificationType;
import com.camploop.service.NotificationService;
import com.camploop.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final ProfileService profileService;
    private final AuthContext authContext;

    @Autowired
    public NotificationController(NotificationService notificationService, ProfileService profileService,
                                  AuthContext authContext) {
        this.notificationService = notificationService;
        this.profileService = profileService;
        this.authContext = authContext;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> latest(HttpServletRequest http) {
        return ResponseEntity.ok(notificationService.latest(me(http)).stream()
                .map(NotificationResponse::new).collect(Collectors.toList()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(HttpServletRequest http) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(me(http))));
    }

    /** Mark notifications read. ?type=BUY_REQUEST limits it to one kind; omit to mark everything. */
    @PostMapping("/read")
    public ResponseEntity<ApiMessageResponse> markRead(@RequestParam(required = false) NotificationType type,
                                                       HttpServletRequest http) {
        notificationService.markRead(me(http), type);
        return ResponseEntity.ok(new ApiMessageResponse("Marked as read"));
    }

    private Profile me(HttpServletRequest http) {
        return profileService.getOrCreate(authContext.require(http), null);
    }
}
