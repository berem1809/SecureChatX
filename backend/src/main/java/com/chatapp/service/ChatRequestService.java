package com.chatapp.service;

import com.chatapp.dto.ChatRequestCreateRequest;
import com.chatapp.dto.ChatRequestResponse;
import com.chatapp.exception.*;
import com.chatapp.model.*;
import com.chatapp.repository.ChatRequestRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * CHAT REQUEST SERVICE - Handles chat request operations
 * ============================================================================
 * 
 * Manages the lifecycle of chat requests:
 * 1. Sending chat requests
 * 2. Accepting chat requests (creates conversation)
 * 3. Rejecting chat requests
 * 4. Listing sent/received requests
 * 
 * BUSINESS RULES:
 * ---------------
 * - Users cannot send requests to themselves
 * - Duplicate requests are not allowed (in either direction)
 * - Only pending requests can be accepted/rejected
 * - Only the receiver can accept/reject a request
 * - Conversation is created only upon acceptance
 */
public interface ChatRequestService {
    
    /**
     * Creates a new chat request from sender to receiver.
     * 
     * @param senderId The ID of the user sending the request
     * @param request The request details
     * @return The created chat request
     */
    ChatRequestResponse createChatRequest(Long senderId, ChatRequestCreateRequest request);
    
    /**
     * Accepts a chat request (receiver only).
     * Creates a conversation between the two users.
     * 
     * @param requestId The chat request ID
     * @param userId The ID of the user accepting (must be receiver)
     * @return The updated chat request
     */
    ChatRequestResponse acceptChatRequest(Long requestId, Long userId);
    
    /**
     * Rejects a chat request (receiver only).
     * 
     * @param requestId The chat request ID
     * @param userId The ID of the user rejecting (must be receiver)
     * @return The updated chat request
     */
    ChatRequestResponse rejectChatRequest(Long requestId, Long userId);
    
    /**
     * Gets all chat requests sent by a user.
     * 
     * @param userId The sender's user ID
     * @return List of sent chat requests
     */
    List<ChatRequestResponse> getSentRequests(Long userId);
    
    /**
     * Gets all chat requests received by a user.
     * 
     * @param userId The receiver's user ID
     * @return List of received chat requests
     */
    List<ChatRequestResponse> getReceivedRequests(Long userId);
    
    /**
     * Gets all pending chat requests received by a user.
     * 
     * @param userId The receiver's user ID
     * @return List of pending chat requests
     */
    List<ChatRequestResponse> getPendingRequests(Long userId);
    
    /**
     * Gets a specific chat request by ID.
     * 
     * @param requestId The chat request ID
     * @param userId The ID of the user requesting (must be sender or receiver)
     * @return The chat request
     */
    ChatRequestResponse getChatRequest(Long requestId, Long userId);
}

/**
 * Implementation of ChatRequestService.
 */
@Service
class ChatRequestServiceImpl implements ChatRequestService {

    private static final Logger logger = LoggerFactory.getLogger(ChatRequestServiceImpl.class);

    private final ChatRequestRepository chatRequestRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;

    public ChatRequestServiceImpl(ChatRequestRepository chatRequestRepository,
                                   ConversationRepository conversationRepository,
                                   UserRepository userRepository,
                                   ConversationService conversationService) {
        this.chatRequestRepository = chatRequestRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.conversationService = conversationService;
    }

    @Override
    @Transactional
    public ChatRequestResponse createChatRequest(Long senderId, ChatRequestCreateRequest request) {
        // Resolve receiver - either by ID or by email
        User receiver;
        
        if (request.getReceiverId() != null) {
            receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new UserNotFoundException(request.getReceiverId()));
        } else if (request.getReceiverEmail() != null && !request.getReceiverEmail().isBlank()) {
            receiver = userRepository.findByEmail(request.getReceiverEmail())
                .orElseThrow(() -> UserNotFoundException.byEmail(request.getReceiverEmail()));
        } else {
            throw new IllegalArgumentException("Either receiverId or receiverEmail must be provided");
        }
        
        Long receiverId = receiver.getId();
        logger.info("Creating chat request from user {} to user {}", senderId, receiverId);
        
        // Validation: Cannot send request to yourself
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send chat request to yourself");
        }
        
        // Check if sender exists
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new UserNotFoundException(senderId));
        
        // Check if conversation already exists
        if (conversationRepository.existsBetweenUsers(senderId, receiverId)) {
            throw new ConversationAlreadyExistsException(senderId, receiverId);
        }
        
        // Check if chat request already exists (in either direction)
        if (chatRequestRepository.existsBetweenUsers(senderId, receiverId)) {
            throw new ChatRequestAlreadyExistsException(senderId, receiverId);
        }
        
        // Create the chat request
        ChatRequest chatRequest = new ChatRequest(sender, receiver);
        ChatRequest savedRequest = chatRequestRepository.save(chatRequest);
        
        logger.info("Chat request created with ID: {}", savedRequest.getId());
        return ChatRequestResponse.fromEntity(savedRequest);
    }

    @Override
    @Transactional
    public ChatRequestResponse acceptChatRequest(Long requestId, Long userId) {
        logger.info("User {} accepting chat request {}", userId, requestId);
        
        ChatRequest chatRequest = validateReceiverAction(requestId, userId);
        
        // Update status to ACCEPTED
        chatRequest.setStatus(ChatRequestStatus.ACCEPTED);
        ChatRequest savedRequest = chatRequestRepository.save(chatRequest);
        
        // Create conversation between the two users
        conversationService.createConversation(
            chatRequest.getSender().getId(), 
            chatRequest.getReceiver().getId()
        );
        
        logger.info("Chat request {} accepted, conversation created", requestId);
        return ChatRequestResponse.fromEntity(savedRequest);
    }

    @Override
    @Transactional
    public ChatRequestResponse rejectChatRequest(Long requestId, Long userId) {
        logger.info("User {} rejecting chat request {}", userId, requestId);
        
        ChatRequest chatRequest = validateReceiverAction(requestId, userId);
        
        // Update status to REJECTED
        chatRequest.setStatus(ChatRequestStatus.REJECTED);
        ChatRequest savedRequest = chatRequestRepository.save(chatRequest);
        
        logger.info("Chat request {} rejected", requestId);
        return ChatRequestResponse.fromEntity(savedRequest);
    }

    @Override
    public List<ChatRequestResponse> getSentRequests(Long userId) {
        logger.debug("Getting sent requests for user {}", userId);
        
        return chatRequestRepository.findBySenderIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(ChatRequestResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<ChatRequestResponse> getReceivedRequests(Long userId) {
        logger.debug("Getting received requests for user {}", userId);
        
        return chatRequestRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(ChatRequestResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<ChatRequestResponse> getPendingRequests(Long userId) {
        logger.debug("Getting pending requests for user {}", userId);
        
        return chatRequestRepository.findByReceiverIdAndStatus(userId, ChatRequestStatus.PENDING)
            .stream()
            .map(ChatRequestResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public ChatRequestResponse getChatRequest(Long requestId, Long userId) {
        logger.debug("Getting chat request {} for user {}", requestId, userId);
        
        ChatRequest chatRequest = chatRequestRepository.findById(requestId)
            .orElseThrow(() -> new ChatRequestNotFoundException(requestId));
        
        // Verify the user is either sender or receiver
        if (!chatRequest.getSender().getId().equals(userId) && 
            !chatRequest.getReceiver().getId().equals(userId)) {
            throw new ChatRequestNotFoundException(requestId);
        }
        
        return ChatRequestResponse.fromEntity(chatRequest);
    }

    /**
     * Validates that a user can perform receiver actions on a chat request.
     * 
     * @param requestId The chat request ID
     * @param userId The user ID attempting the action
     * @return The chat request if valid
     */
    private ChatRequest validateReceiverAction(Long requestId, Long userId) {
        ChatRequest chatRequest = chatRequestRepository.findById(requestId)
            .orElseThrow(() -> new ChatRequestNotFoundException(requestId));
        
        // Only the receiver can accept/reject
        if (!chatRequest.getReceiver().getId().equals(userId)) {
            throw new ConversationAccessDeniedException("Only the receiver can accept or reject a chat request");
        }
        
        // Only pending requests can be accepted/rejected
        if (chatRequest.getStatus() != ChatRequestStatus.PENDING) {
            throw new IllegalStateException("Chat request has already been " + chatRequest.getStatus().name().toLowerCase());
        }
        
        return chatRequest;
    }
}
