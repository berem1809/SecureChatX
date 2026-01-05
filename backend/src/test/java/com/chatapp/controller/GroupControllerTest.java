package com.chatapp.controller;

import com.chatapp.dto.*;
import com.chatapp.model.*;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.GroupInvitationService;
import com.chatapp.service.GroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for GroupController.
 * Tests all group and invitation REST endpoints.
 */
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GroupInvitationService invitationService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private User otherUser;
    private Group testGroup;
    private GroupResponse testGroupResponse;
    private GroupMemberResponse testMemberResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new GroupController(groupService, invitationService, userRepository))
            .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Setup test users
        testUser = new User("test@example.com", "password", "Test User", "ACTIVE", List.of("ROLE_USER"));
        testUser.setId(1L);

        otherUser = new User("other@example.com", "password", "Other User", "ACTIVE", List.of("ROLE_USER"));
        otherUser.setId(2L);

        // Setup test group
        testGroup = new Group("Test Group", "A test group", testUser);
        testGroup.setId(100L);

        // Setup test member
        GroupMember adminMember = new GroupMember();
        adminMember.setId(1L);
        adminMember.setGroup(testGroup);
        adminMember.setUser(testUser);
        adminMember.setRole(GroupRole.ADMIN);
        adminMember.setJoinedAt(LocalDateTime.now());

        testGroup.setMembers(List.of(adminMember));

        // Create response DTOs using factory methods
        testMemberResponse = GroupMemberResponse.fromEntity(adminMember);
        testGroupResponse = GroupResponse.fromEntityWithMembers(testGroup);
    }

    private UsernamePasswordAuthenticationToken createAuthentication() {
        return new UsernamePasswordAuthenticationToken(
            "test@example.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // ========================================================================
    // CREATE GROUP TESTS
    // ========================================================================

    @Nested
    @DisplayName("POST /api/groups")
    class CreateGroupEndpoint {

        @Test
        @DisplayName("Successfully creates group")
        void createGroup_Success() throws Exception {
            // Arrange
            GroupCreateRequest request = new GroupCreateRequest();
            request.setName("New Group");
            request.setDescription("A new group");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(groupService.createGroup(eq(1L), any(GroupCreateRequest.class)))
                .thenReturn(testGroupResponse);

            // Act & Assert
            mockMvc.perform(post("/api/groups")
                    .principal(createAuthentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.name").value("Test Group"));

            verify(groupService).createGroup(eq(1L), any(GroupCreateRequest.class));
        }
    }

    // ========================================================================
    // GET GROUPS TESTS
    // ========================================================================

    @Nested
    @DisplayName("GET /api/groups")
    class GetGroupsEndpoints {

        @Test
        @DisplayName("Gets all user groups")
        void getUserGroups_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(groupService.getUserGroups(1L)).thenReturn(List.of(testGroupResponse));

            // Act & Assert
            mockMvc.perform(get("/api/groups")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(100L));

            verify(groupService).getUserGroups(1L);
        }

        @Test
        @DisplayName("Gets specific group")
        void getGroup_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(groupService.getGroup(100L, 1L)).thenReturn(testGroupResponse);

            // Act & Assert
            mockMvc.perform(get("/api/groups/100")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.name").value("Test Group"));

            verify(groupService).getGroup(100L, 1L);
        }
    }

    // ========================================================================
    // UPDATE GROUP TESTS
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/groups/{id}")
    class UpdateGroupEndpoint {

        @Test
        @DisplayName("Successfully updates group")
        void updateGroup_Success() throws Exception {
            // Arrange
            GroupCreateRequest request = new GroupCreateRequest();
            request.setName("Updated Name");
            request.setDescription("Updated description");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(groupService.updateGroup(100L, 1L, "Updated Name", "Updated description"))
                .thenReturn(testGroupResponse);

            // Act & Assert
            mockMvc.perform(put("/api/groups/100")
                    .principal(createAuthentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            verify(groupService).updateGroup(100L, 1L, "Updated Name", "Updated description");
        }
    }

    // ========================================================================
    // LEAVE GROUP TESTS
    // ========================================================================

    @Nested
    @DisplayName("POST /api/groups/{id}/leave")
    class LeaveGroupEndpoint {

        @Test
        @DisplayName("Successfully leaves group")
        void leaveGroup_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // Act & Assert
            mockMvc.perform(post("/api/groups/100/leave")
                    .principal(createAuthentication()))
                .andExpect(status().isNoContent());

            verify(groupService).leaveGroup(100L, 1L);
        }
    }

    // ========================================================================
    // MEMBER MANAGEMENT TESTS
    // ========================================================================

    @Nested
    @DisplayName("Member Management Endpoints")
    class MemberManagementEndpoints {

        @Test
        @DisplayName("Gets group members")
        void getGroupMembers_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(groupService.getGroupMembers(100L, 1L)).thenReturn(List.of(testMemberResponse));

            // Act & Assert
            mockMvc.perform(get("/api/groups/100/members")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].user.id").value(1L));

            verify(groupService).getGroupMembers(100L, 1L);
        }

        @Test
        @DisplayName("Removes member")
        void removeMember_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // Act & Assert
            mockMvc.perform(delete("/api/groups/100/members/2")
                    .principal(createAuthentication()))
                .andExpect(status().isNoContent());

            verify(groupService).removeMember(100L, 1L, 2L);
        }

        @Test
        @DisplayName("Promotes member")
        void promoteMember_Success() throws Exception {
            // Arrange
            // Create a promoted member for the response
            GroupMember promotedMember = new GroupMember();
            promotedMember.setId(2L);
            promotedMember.setGroup(testGroup);
            promotedMember.setUser(otherUser);
            promotedMember.setRole(GroupRole.ADMIN);
            promotedMember.setJoinedAt(LocalDateTime.now());
            GroupMemberResponse promotedResponse = GroupMemberResponse.fromEntity(promotedMember);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(groupService.promoteMember(100L, 1L, 2L)).thenReturn(promotedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/groups/100/members/2/promote")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

            verify(groupService).promoteMember(100L, 1L, 2L);
        }
    }

    // ========================================================================
    // INVITATION MANAGEMENT TESTS
    // ========================================================================

    @Nested
    @DisplayName("Invitation Management Endpoints")
    class InvitationManagementEndpoints {

        private GroupInvitationResponse testInvitationResponse;
        private GroupInvitation testInvitation;

        @BeforeEach
        void setUpInvitation() {
            // Create actual entity for the response
            testInvitation = new GroupInvitation(testGroup, testUser, otherUser);
            testInvitation.setId(200L);
            testInvitationResponse = GroupInvitationResponse.fromEntity(testInvitation);
        }

        @Test
        @DisplayName("Creates invitation")
        void createInvitation_Success() throws Exception {
            // Arrange
            GroupInviteRequest request = new GroupInviteRequest();
            request.setInviteeId(2L);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(invitationService.createInvitation(eq(100L), eq(1L), any(GroupInviteRequest.class)))
                .thenReturn(testInvitationResponse);

            // Act & Assert
            mockMvc.perform(post("/api/groups/100/invitations")
                    .principal(createAuthentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(200L));

            verify(invitationService).createInvitation(eq(100L), eq(1L), any(GroupInviteRequest.class));
        }

        @Test
        @DisplayName("Gets pending invitations")
        void getPendingInvitations_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(invitationService.getPendingInvitations(1L))
                .thenReturn(List.of(testInvitationResponse));

            // Act & Assert
            mockMvc.perform(get("/api/groups/invitations/pending")
                    .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

            verify(invitationService).getPendingInvitations(1L);
        }

        @Test
        @DisplayName("Accepts invitation")
        void acceptInvitation_Success() throws Exception {
            // Arrange
            GroupInvitationActionRequest actionRequest = new GroupInvitationActionRequest();
            actionRequest.setAction("ACCEPT");

            testInvitation.setStatus(GroupInvitationStatus.ACCEPTED);
            GroupInvitationResponse acceptedResponse = GroupInvitationResponse.fromEntity(testInvitation);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(invitationService.acceptInvitation(200L, 1L)).thenReturn(acceptedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/groups/invitations/200/action")
                    .principal(createAuthentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(actionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

            verify(invitationService).acceptInvitation(200L, 1L);
        }

        @Test
        @DisplayName("Rejects invitation")
        void rejectInvitation_Success() throws Exception {
            // Arrange
            GroupInvitationActionRequest actionRequest = new GroupInvitationActionRequest();
            actionRequest.setAction("REJECT");

            testInvitation.setStatus(GroupInvitationStatus.REJECTED);
            GroupInvitationResponse rejectedResponse = GroupInvitationResponse.fromEntity(testInvitation);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(invitationService.rejectInvitation(200L, 1L)).thenReturn(rejectedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/groups/invitations/200/action")
                    .principal(createAuthentication())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(actionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(invitationService).rejectInvitation(200L, 1L);
        }

        @Test
        @DisplayName("Cancels invitation")
        void cancelInvitation_Success() throws Exception {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // Act & Assert
            mockMvc.perform(delete("/api/groups/invitations/200")
                    .principal(createAuthentication()))
                .andExpect(status().isNoContent());

            verify(invitationService).cancelInvitation(200L, 1L);
        }
    }
}
