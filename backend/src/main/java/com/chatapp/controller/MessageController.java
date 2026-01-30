package com.chatapp.controller;

import com.chatapp.dto.MessageCreateRequest;
import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.ErrorResponse;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.MessageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for message operations within conversations.
 * 
 * REST Endpoints:
 * ---------------
 * GET  /api/conversations/{id}/messages - Get all messages in a conversation
 * POST /api/conversations/{id}/messages - Send a message to a conversation
 */
@RestController
@RequestMapping("/api/conversations")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;
    private final UserRepository userRepository;

    public MessageController(MessageService messageService, UserRepository userRepository) {
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    /**
     * Gets all messages in a conversation.
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.debug("User {} fetching messages for conversation {}", userId, conversationId);
        
        List<MessageResponse> messages = messageService.getMessages(conversationId, userId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Sends a message to a conversation.
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody MessageCreateRequest request,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        logger.info("User {} sending message to conversation {}", userId, conversationId);
        logger.debug("Message request: encrypted={}, hasNonce={}", 
            request.getEncryptedContent() != null,
            request.getEncryptionNonce() != null);
        
        try {
            MessageResponse message = messageService.sendMessage(conversationId, userId, request);
            logger.info("✅ Message sent successfully by user {} in conversation {}", userId, conversationId);
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (IllegalArgumentException e) {
            logger.warn("❌ Invalid argument when sending message: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Invalid message request", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Error sending message in conversation {}: {}", conversationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Failed to send message", e.getMessage()));
        }
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"))
            .getId();
    }
}
