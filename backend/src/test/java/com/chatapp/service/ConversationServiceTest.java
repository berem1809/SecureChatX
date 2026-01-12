package com.chatapp.service;

import com.chatapp.dto.ConversationResponse;
import com.chatapp.exception.ConversationAccessDeniedException;
import com.chatapp.exception.ConversationAlreadyExistsException;
import com.chatapp.exception.ConversationNotFoundException;
import com.chatapp.model.Conversation;
import com.chatapp.model.ConversationMember;
import com.chatapp.model.User;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.util.ConversationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConversationService.
 * Tests all conversation operations including creation and retrieval.
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationValidator conversationValidator;

    private ConversationService conversationService;

    private User user1;
    private User user2;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationServiceImpl(
            conversationRepository,
            conversationMemberRepository,
            userRepository,
            conversationValidator
        );

        // Setup test users
        user1 = new User("user1@test.com", "password", "User One", "ACTIVE", List.of("ROLE_USER"));
        user1.setId(1L);

        user2 = new User("user2@test.com", "password", "User Two", "ACTIVE", List.of("ROLE_USER"));
        user2.setId(2L);

        // Setup test conversation (user1 < user2 since user1.id=1 < user2.id=2)
        conversation = new Conversation(user1, user2);
        conversation.setId(100L);
    }

    // ========================================================================
    // CREATE CONVERSATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Create Conversation")
    class CreateConversationTests {

        @Test
        @DisplayName("Successfully creates conversation")
        void createConversation_Success() {
            // Arrange
            when(conversationRepository.existsBetweenUsers(1L, 2L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
                Conversation saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            // Act
            ConversationResponse response = conversationService.createConversation(1L, 2L);

            // Assert
            assertNotNull(response);
            verify(conversationRepository).save(any(Conversation.class));
            verify(conversationMemberRepository, times(2)).save(any(ConversationMember.class));
        }

        @Test
        @DisplayName("Throws exception when conversation already exists")
        void createConversation_AlreadyExists_ThrowsException() {
            // Arrange
            when(conversationRepository.existsBetweenUsers(1L, 2L)).thenReturn(true);

            // Act & Assert
            assertThrows(ConversationAlreadyExistsException.class,
                () -> conversationService.createConversation(1L, 2L));
            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws exception when creating conversation with self")
        void createConversation_WithSelf_ThrowsException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> conversationService.createConversation(1L, 1L));
            verify(conversationRepository, never()).save(any());
        }
    }

    // ========================================================================
    // GET CONVERSATIONS TESTS
    // ========================================================================

    @Nested
    @DisplayName("Get Conversations")
    class GetConversationsTests {

        @Test
        @DisplayName("Successfully gets user conversations")
        void getUserConversations_Success() {
            // Arrange
            when(conversationRepository.findByUserId(1L)).thenReturn(List.of(conversation));

            // Act
            List<ConversationResponse> responses = conversationService.getUserConversations(1L);

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(100L, responses.get(0).getId());
        }

        @Test
        @DisplayName("Returns empty list when no conversations")
        void getUserConversations_Empty() {
            // Arrange
            when(conversationRepository.findByUserId(1L)).thenReturn(List.of());

            // Act
            List<ConversationResponse> responses = conversationService.getUserConversations(1L);

            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
        }

        @Test
        @DisplayName("Successfully gets specific conversation")
        void getConversation_Success() {
            // Arrange
            when(conversationValidator.validateAndGetConversation(100L, 1L)).thenReturn(conversation);

            // Act
            ConversationResponse response = conversationService.getConversation(100L, 1L);

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getId());
        }

        @Test
        @DisplayName("Gets conversation between two users")
        void getConversationBetweenUsers_Success() {
            // Arrange
            when(conversationRepository.findBetweenUsers(1L, 2L)).thenReturn(Optional.of(conversation));

            // Act
            ConversationResponse response = conversationService.getConversationBetweenUsers(1L, 2L);

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getId());
        }

        @Test
        @DisplayName("Returns null when no conversation between users")
        void getConversationBetweenUsers_NotFound() {
            // Arrange
            when(conversationRepository.findBetweenUsers(1L, 3L)).thenReturn(Optional.empty());

            // Act
            ConversationResponse response = conversationService.getConversationBetweenUsers(1L, 3L);

            // Assert
            assertNull(response);
        }
    }

    // ========================================================================
    // CONVERSATION EXISTS TESTS
    // ========================================================================

    @Nested
    @DisplayName("Conversation Exists")
    class ConversationExistsTests {

        @Test
        @DisplayName("Returns true when conversation exists")
        void conversationExists_True() {
            // Arrange
            when(conversationRepository.existsBetweenUsers(1L, 2L)).thenReturn(true);

            // Act
            boolean exists = conversationService.conversationExists(1L, 2L);

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("Returns false when conversation does not exist")
        void conversationExists_False() {
            // Arrange
            when(conversationRepository.existsBetweenUsers(1L, 3L)).thenReturn(false);

            // Act
            boolean exists = conversationService.conversationExists(1L, 3L);

            // Assert
            assertFalse(exists);
        }
    }
}
