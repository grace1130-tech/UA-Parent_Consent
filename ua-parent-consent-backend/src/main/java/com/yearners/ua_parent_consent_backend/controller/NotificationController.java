
package com.yearners.ua_parent_consent_backend.controller;

import com.yearners.ua_parent_consent_backend.dto.NotificationResponse;
import com.yearners.ua_parent_consent_backend.model.Notification;
import com.yearners.ua_parent_consent_backend.model.User;
import com.yearners.ua_parent_consent_backend.repository.UserRepository;
import com.yearners.ua_parent_consent_backend.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedParent(Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }

        String email = authentication.getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found."
                ));

        if (!"PARENT".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only parent accounts can access these notifications."
            );
        }

        return user;
    }

    // Get all notifications.
    @GetMapping
    public List<NotificationResponse> getNotifications(
            Authentication authentication
    ) {
        User parent = getAuthenticatedParent(authentication);

        return notificationService
                .getUserNotifications(parent.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // Get unread notification count.
    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(
            Authentication authentication
    ) {
        User parent = getAuthenticatedParent(authentication);

        return Map.of(
                "unreadCount",
                notificationService.getUnreadCount(parent.getId())
        );
    }

    // Mark one notification as read.
    @PutMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User parent = getAuthenticatedParent(authentication);

        Notification notification =
                notificationService.markAsRead(id, parent.getId());

        return NotificationResponse.from(notification);
    }

    // Mark all notifications as read.
    @PutMapping("/read-all")
    public Map<String, String> markAllAsRead(
            Authentication authentication
    ) {
        User parent = getAuthenticatedParent(authentication);

        notificationService.markAllAsRead(parent.getId());

        return Map.of(
                "message",
                "All notifications marked as read."
        );
    }
}
