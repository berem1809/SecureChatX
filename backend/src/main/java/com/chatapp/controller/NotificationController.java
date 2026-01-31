package com.chatapp.controller;

import com.chatapp.dto.NotificationCountResponse;
import com.chatapp.exception.UserNotFoundException;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * NOTIFICATION CONTROLLER - Handles notification endpoints
 * ============================================================================
 * 
 * REST Endpoints:
 * ---------------
 * GET /api/notifications/counts - Get all notification counts for current user
 * 
 * SECURITY:
 * ---------
 * - All endpoints require valid JWT token
 * - User ID is extracted from JWT
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Gets all notification counts for the current user.
     * 
     * GET /api/notifications/counts
     * 
     * @param authentication The authentication object
     * @return Notification counts (unread messages, pending requests, etc.)
     */
    @GetMapping("/counts")
    public ResponseEntity<NotificationCountResponse> getNotificationCounts(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting notification counts for user {}", userId);
        
        NotificationCountResponse counts = notificationService.getNotificationCounts(userId);
        return ResponseEntity.ok(counts);
    }

    /**
     * Extracts the user ID from the authentication object.
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> UserNotFoundException.byEmail(email))
            .getId();
    }
}
