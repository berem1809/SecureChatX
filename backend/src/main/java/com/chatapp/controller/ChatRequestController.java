package com.chatapp.controller;

import com.chatapp.dto.ChatRequestActionRequest;
import com.chatapp.dto.ChatRequestCreateRequest;
import com.chatapp.dto.ChatRequestResponse;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.ChatRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * CHAT REQUEST CONTROLLER - Handles chat request endpoints
 * ============================================================================
 * 
 * REST Endpoints:
 * ---------------
 * POST /api/chat-requests                    - Send a chat request
 * GET  /api/chat-requests/sent               - Get sent requests
 * GET  /api/chat-requests/received           - Get received requests
 * GET  /api/chat-requests/pending            - Get pending requests (received)
 * GET  /api/chat-requests/{id}               - Get specific request
 * POST /api/chat-requests/{id}/action        - Accept or reject request
 * 
 * SECURITY:
 * ---------
 * - All endpoints require valid JWT token
 * - Sender ID is extracted from JWT, not from request body
 * - Only the receiver can accept/reject a request
 */
@RestController
@RequestMapping("/api/chat-requests")
public class ChatRequestController {

    private static final Logger logger = LoggerFactory.getLogger(ChatRequestController.class);

    private final ChatRequestService chatRequestService;
    private final UserRepository userRepository;

    public ChatRequestController(ChatRequestService chatRequestService, UserRepository userRepository) {
        this.chatRequestService = chatRequestService;
        this.userRepository = userRepository;
    }

    /**
     * Sends a new chat request to another user.
     * 
     * POST /api/chat-requests
     * Body: { "receiverId": 123 }
     * 
     * @param request The chat request details
     * @param authentication The authentication object
     * @return The created chat request
     */
    @PostMapping
    public ResponseEntity<ChatRequestResponse> createChatRequest(
            @Valid @RequestBody ChatRequestCreateRequest request,
            Authentication authentication) {
        
        Long senderId = getUserIdFromAuth(authentication);
        logger.info("User {} sending chat request to user {}", senderId, request.getReceiverId());
        
        ChatRequestResponse response = chatRequestService.createChatRequest(senderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets all chat requests sent by the current user.
     * 
     * GET /api/chat-requests/sent
     * 
     * @param authentication The authentication object
     * @return List of sent chat requests
     */
    @GetMapping("/sent")
    public ResponseEntity<List<ChatRequestResponse>> getSentRequests(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting sent requests for user {}", userId);
        
        List<ChatRequestResponse> requests = chatRequestService.getSentRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Gets all chat requests received by the current user.
     * 
     * GET /api/chat-requests/received
     * 
     * @param authentication The authentication object
     * @return List of received chat requests
     */
    @GetMapping("/received")
    public ResponseEntity<List<ChatRequestResponse>> getReceivedRequests(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting received requests for user {}", userId);
        
        List<ChatRequestResponse> requests = chatRequestService.getReceivedRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Gets all pending chat requests received by the current user.
     * 
     * GET /api/chat-requests/pending
     * 
     * @param authentication The authentication object
     * @return List of pending chat requests
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ChatRequestResponse>> getPendingRequests(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting pending requests for user {}", userId);
        
        List<ChatRequestResponse> requests = chatRequestService.getPendingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Gets a specific chat request by ID.
     * 
     * GET /api/chat-requests/{requestId}
     * 
     * @param requestId The chat request ID
     * @param authentication The authentication object
     * @return The chat request
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<ChatRequestResponse> getChatRequest(
            @PathVariable Long requestId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("Getting chat request {} for user {}", requestId, userId);
        
        ChatRequestResponse request = chatRequestService.getChatRequest(requestId, userId);
        return ResponseEntity.ok(request);
    }

    /**
     * Accepts or rejects a chat request.
     * 
     * POST /api/chat-requests/{requestId}/action
     * Body: { "action": "ACCEPT" } or { "action": "REJECT" }
     * 
     * @param requestId The chat request ID
     * @param actionRequest The action to perform
     * @param authentication The authentication object
     * @return The updated chat request
     */
    @PostMapping("/{requestId}/action")
    public ResponseEntity<ChatRequestResponse> handleChatRequestAction(
            @PathVariable Long requestId,
            @Valid @RequestBody ChatRequestActionRequest actionRequest,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.info("User {} performing action {} on chat request {}", 
                   userId, actionRequest.getAction(), requestId);
        
        ChatRequestResponse response;
        if (actionRequest.isAccept()) {
            response = chatRequestService.acceptChatRequest(requestId, userId);
        } else {
            response = chatRequestService.rejectChatRequest(requestId, userId);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the user ID from the authentication object.
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }
}
