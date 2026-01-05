package com.chatapp.service;

import com.chatapp.dto.ConversationResponse;
import com.chatapp.exception.*;
import com.chatapp.model.*;
import com.chatapp.repository.*;
import com.chatapp.util.ConversationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * CONVERSATION SERVICE - Handles one-to-one conversation operations
 * ============================================================================
 * 
 * Manages conversations between users:
 * 1. Creating conversations (only through chat request acceptance)
 * 2. Retrieving user's conversations
 * 3. Getting conversation details
 * 
 * BUSINESS RULES:
 * ---------------
 * - Conversations are created only after chat request acceptance
 * - Each pair of users can have only one conversation
 * - User IDs are normalized (user1Id < user2Id) to prevent duplicates
 * - Only participants can access conversation details
 */
public interface ConversationService {
    
    /**
     * Creates a new conversation between two users.
     * Should only be called after a chat request is accepted.
     * 
     * @param user1Id First user's ID
     * @param user2Id Second user's ID
     * @return The created conversation
     */
    ConversationResponse createConversation(Long user1Id, Long user2Id);
    
    /**
     * Gets all conversations for a user.
     * 
     * @param userId The user's ID
     * @return List of conversations
     */
    List<ConversationResponse> getUserConversations(Long userId);
    
    /**
     * Gets a specific conversation by ID.
     * Validates that the requesting user is a participant.
     * 
     * @param conversationId The conversation ID
     * @param userId The ID of the user requesting
     * @return The conversation
     */
    ConversationResponse getConversation(Long conversationId, Long userId);
    
    /**
     * Gets a conversation between two specific users.
     * 
     * @param user1Id First user's ID
     * @param user2Id Second user's ID
     * @return The conversation, or null if not found
     */
    ConversationResponse getConversationBetweenUsers(Long user1Id, Long user2Id);
    
    /**
     * Checks if a conversation exists between two users.
     * 
     * @param user1Id First user's ID
     * @param user2Id Second user's ID
     * @return true if conversation exists
     */
    boolean conversationExists(Long user1Id, Long user2Id);
}

/**
 * Implementation of ConversationService.
 */
@Service
class ConversationServiceImpl implements ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final ConversationValidator conversationValidator;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                    ConversationMemberRepository conversationMemberRepository,
                                    UserRepository userRepository,
                                    ConversationValidator conversationValidator) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.conversationValidator = conversationValidator;
    }

    @Override
    @Transactional
    public ConversationResponse createConversation(Long user1Id, Long user2Id) {
        logger.info("Creating conversation between users {} and {}", user1Id, user2Id);
        
        // Cannot create conversation with yourself
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Cannot create conversation with yourself");
        }
        
        // Check if conversation already exists
        if (conversationRepository.existsBetweenUsers(user1Id, user2Id)) {
            throw new ConversationAlreadyExistsException(user1Id, user2Id);
        }
        
        // Get users
        User user1 = userRepository.findById(user1Id)
            .orElseThrow(() -> new UserNotFoundException(user1Id));
        User user2 = userRepository.findById(user2Id)
            .orElseThrow(() -> new UserNotFoundException(user2Id));
        
        // Create conversation (constructor handles ID normalization)
        Conversation conversation = new Conversation(user1, user2);
        Conversation savedConversation = conversationRepository.save(conversation);
        
        // Create conversation members
        ConversationMember member1 = new ConversationMember(savedConversation, user1);
        ConversationMember member2 = new ConversationMember(savedConversation, user2);
        conversationMemberRepository.save(member1);
        conversationMemberRepository.save(member2);
        
        logger.info("Conversation created with ID: {}", savedConversation.getId());
        return ConversationResponse.fromEntity(savedConversation);
    }

    @Override
    public List<ConversationResponse> getUserConversations(Long userId) {
        logger.debug("Getting conversations for user {}", userId);
        
        return conversationRepository.findByUserId(userId).stream()
            .map(conv -> ConversationResponse.fromEntityWithCurrentUser(conv, userId))
            .collect(Collectors.toList());
    }

    @Override
    public ConversationResponse getConversation(Long conversationId, Long userId) {
        logger.debug("Getting conversation {} for user {}", conversationId, userId);
        
        // Validate user is a participant and get conversation
        Conversation conversation = conversationValidator.validateAndGetConversation(conversationId, userId);
        
        return ConversationResponse.fromEntityWithCurrentUser(conversation, userId);
    }

    @Override
    public ConversationResponse getConversationBetweenUsers(Long user1Id, Long user2Id) {
        logger.debug("Getting conversation between users {} and {}", user1Id, user2Id);
        
        return conversationRepository.findBetweenUsers(user1Id, user2Id)
            .map(ConversationResponse::fromEntity)
            .orElse(null);
    }

    @Override
    public boolean conversationExists(Long user1Id, Long user2Id) {
        return conversationRepository.existsBetweenUsers(user1Id, user2Id);
    }
}
