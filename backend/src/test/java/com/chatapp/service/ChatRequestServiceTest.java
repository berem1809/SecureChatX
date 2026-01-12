package com.chatapp.service;

import com.chatapp.dto.ChatRequestCreateRequest;
import com.chatapp.dto.ChatRequestResponse;
import com.chatapp.exception.*;
import com.chatapp.model.ChatRequest;
import com.chatapp.model.ChatRequestStatus;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRequestRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.UserRepository;
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
 * Unit tests for ChatRequestService.
 * Tests all chat request operations including create, accept, reject, and retrieval.
 */
@ExtendWith(MockitoExtension.class)
class ChatRequestServiceTest {

    @Mock
    private ChatRequestRepository chatRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationService conversationService;

    private ChatRequestService chatRequestService;

    private User sender;
    private User receiver;
    private ChatRequest pendingRequest;

    @BeforeEach
    void setUp() {
        chatRequestService = new ChatRequestServiceImpl(
            chatRequestRepository,
            conversationRepository,
            userRepository,
            conversationService
        );

        // Setup test users
        sender = new User("sender@test.com", "password", "Sender", "ACTIVE", List.of("ROLE_USER"));
        sender.setId(1L);

        receiver = new User("receiver@test.com", "password", "Receiver", "ACTIVE", List.of("ROLE_USER"));
        receiver.setId(2L);

        // Setup test chat request
        pendingRequest = new ChatRequest();
        pendingRequest.setId(100L);
        pendingRequest.setSender(sender);
        pendingRequest.setReceiver(receiver);
        pendingRequest.setStatus(ChatRequestStatus.PENDING);
        pendingRequest.setCreatedAt(LocalDateTime.now());
    }

    // ========================================================================
    // CREATE CHAT REQUEST TESTS
    // ========================================================================

    @Nested
    @DisplayName("Create Chat Request")
    class CreateChatRequestTests {

        @Test
        @DisplayName("Successfully creates chat request")
        void createChatRequest_Success() {
            // Arrange
            ChatRequestCreateRequest request = new ChatRequestCreateRequest();
            request.setReceiverId(2L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(chatRequestRepository.existsBetweenUsers(1L, 2L)).thenReturn(false);
            when(conversationRepository.existsBetweenUsers(1L, 2L)).thenReturn(false);
            when(chatRequestRepository.save(any(ChatRequest.class))).thenAnswer(invocation -> {
                ChatRequest saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            // Act
            ChatRequestResponse response = chatRequestService.createChatRequest(1L, request);

            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getSender().getId());
            assertEquals(2L, response.getReceiver().getId());
            assertEquals(ChatRequestStatus.PENDING, response.getStatus());
            verify(chatRequestRepository).save(any(ChatRequest.class));
        }

        @Test
        @DisplayName("Throws exception when sending request to self")
        void createChatRequest_ToSelf_ThrowsException() {
            // Arrange
            ChatRequestCreateRequest request = new ChatRequestCreateRequest();
            request.setReceiverId(1L); // Same as sender

            // Mock receiver lookup (which happens first)
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> chatRequestService.createChatRequest(1L, request));
            verify(chatRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws exception when receiver not found")
        void createChatRequest_ReceiverNotFound_ThrowsException() {
            // Arrange
            ChatRequestCreateRequest request = new ChatRequestCreateRequest();
            request.setReceiverId(999L);

            // Only mock receiver lookup - sender is checked after receiver is found
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UserNotFoundException.class,
                () -> chatRequestService.createChatRequest(1L, request));
            verify(chatRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws exception when chat request already exists")
        void createChatRequest_AlreadyExists_ThrowsException() {
            // Arrange
            ChatRequestCreateRequest request = new ChatRequestCreateRequest();
            request.setReceiverId(2L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(conversationRepository.existsBetweenUsers(1L, 2L)).thenReturn(false);
            when(chatRequestRepository.existsBetweenUsers(1L, 2L)).thenReturn(true);

            // Act & Assert
            assertThrows(ChatRequestAlreadyExistsException.class,
                () -> chatRequestService.createChatRequest(1L, request));
            verify(chatRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws exception when conversation already exists")
        void createChatRequest_ConversationExists_ThrowsException() {
            // Arrange
            ChatRequestCreateRequest request = new ChatRequestCreateRequest();
            request.setReceiverId(2L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(conversationRepository.existsBetweenUsers(1L, 2L)).thenReturn(true);

            // Act & Assert
            assertThrows(ConversationAlreadyExistsException.class,
                () -> chatRequestService.createChatRequest(1L, request));
            verify(chatRequestRepository, never()).save(any());
        }
    }

    // ========================================================================
    // ACCEPT CHAT REQUEST TESTS
    // ========================================================================

    @Nested
    @DisplayName("Accept Chat Request")
    class AcceptChatRequestTests {

        @Test
        @DisplayName("Successfully accepts chat request")
        void acceptChatRequest_Success() {
            // Arrange
            when(chatRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
            when(chatRequestRepository.save(any(ChatRequest.class))).thenReturn(pendingRequest);

            // Act
            ChatRequestResponse response = chatRequestService.acceptChatRequest(100L, 2L);

            // Assert
            assertNotNull(response);
            assertEquals(ChatRequestStatus.ACCEPTED, response.getStatus());
            verify(conversationService).createConversation(1L, 2L);
            verify(chatRequestRepository).save(any(ChatRequest.class));
        }

        @Test
        @DisplayName("Throws exception when request not found")
        void acceptChatRequest_NotFound_ThrowsException() {
            // Arrange
            when(chatRequestRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ChatRequestNotFoundException.class,
                () -> chatRequestService.acceptChatRequest(999L, 2L));
        }

        @Test
        @DisplayName("Throws exception when user is not receiver")
        void acceptChatRequest_NotReceiver_ThrowsException() {
            // Arrange
            when(chatRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

            // Act & Assert - User 1 (sender) trying to accept
            assertThrows(ConversationAccessDeniedException.class,
                () -> chatRequestService.acceptChatRequest(100L, 1L));
            verify(conversationService, never()).createConversation(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Throws exception when request is not pending")
        void acceptChatRequest_NotPending_ThrowsException() {
            // Arrange
            pendingRequest.setStatus(ChatRequestStatus.ACCEPTED);
            when(chatRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

            // Act & Assert
            assertThrows(IllegalStateException.class,
                () -> chatRequestService.acceptChatRequest(100L, 2L));
        }
    }

    // ========================================================================
    // REJECT CHAT REQUEST TESTS
    // ========================================================================

    @Nested
    @DisplayName("Reject Chat Request")
    class RejectChatRequestTests {

        @Test
        @DisplayName("Successfully rejects chat request")
        void rejectChatRequest_Success() {
            // Arrange
            when(chatRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
            when(chatRequestRepository.save(any(ChatRequest.class))).thenReturn(pendingRequest);

            // Act
            ChatRequestResponse response = chatRequestService.rejectChatRequest(100L, 2L);

            // Assert
            assertNotNull(response);
            assertEquals(ChatRequestStatus.REJECTED, response.getStatus());
            verify(conversationService, never()).createConversation(anyLong(), anyLong());
            verify(chatRequestRepository).save(any(ChatRequest.class));
        }

        @Test
        @DisplayName("Throws exception when user is not receiver")
        void rejectChatRequest_NotReceiver_ThrowsException() {
            // Arrange
            when(chatRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

            // Act & Assert - User 1 (sender) trying to reject
            assertThrows(ConversationAccessDeniedException.class,
                () -> chatRequestService.rejectChatRequest(100L, 1L));
        }
    }

    // ========================================================================
    // GET CHAT REQUESTS TESTS
    // ========================================================================

    @Nested
    @DisplayName("Get Chat Requests")
    class GetChatRequestsTests {

        @Test
        @DisplayName("Successfully gets sent requests")
        void getSentRequests_Success() {
            // Arrange
            when(chatRequestRepository.findBySenderIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(pendingRequest));

            // Act
            List<ChatRequestResponse> responses = chatRequestService.getSentRequests(1L);

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(1L, responses.get(0).getSender().getId());
        }

        @Test
        @DisplayName("Successfully gets received requests")
        void getReceivedRequests_Success() {
            // Arrange
            when(chatRequestRepository.findByReceiverIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(pendingRequest));

            // Act
            List<ChatRequestResponse> responses = chatRequestService.getReceivedRequests(2L);

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(2L, responses.get(0).getReceiver().getId());
        }

        @Test
        @DisplayName("Successfully gets pending requests")
        void getPendingRequests_Success() {
            // Arrange
            when(chatRequestRepository.findByReceiverIdAndStatus(2L, ChatRequestStatus.PENDING))
                .thenReturn(List.of(pendingRequest));

            // Act
            List<ChatRequestResponse> responses = chatRequestService.getPendingRequests(2L);

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(ChatRequestStatus.PENDING, responses.get(0).getStatus());
        }

        @Test
        @DisplayName("Successfully gets specific chat request as sender")
        void getChatRequest_AsSender_Success() {
            // Arrange
            when(chatRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

            // Act
            ChatRequestResponse response = chatRequestService.getChatRequest(100L, 1L);

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getId());
        }

        @Test
        @DisplayName("Successfully gets specific chat request as receiver")
        void getChatRequest_AsReceiver_Success() {
            // Arrange
            when(chatRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

            // Act
            ChatRequestResponse response = chatRequestService.getChatRequest(100L, 2L);

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getId());
        }

        @Test
        @DisplayName("Throws exception when accessing others' request")
        void getChatRequest_NotParticipant_ThrowsException() {
            // Arrange
            when(chatRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

            // Act & Assert - User 3 trying to access
            assertThrows(ChatRequestNotFoundException.class,
                () -> chatRequestService.getChatRequest(100L, 3L));
        }
    }
}
