package com.chatapp.service;

import com.chatapp.dto.GroupCreateRequest;
import com.chatapp.dto.GroupMemberResponse;
import com.chatapp.dto.GroupResponse;
import com.chatapp.exception.GroupAccessDeniedException;
import com.chatapp.exception.GroupNotFoundException;
import com.chatapp.model.*;
import com.chatapp.repository.GroupMemberRepository;
import com.chatapp.repository.GroupRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.util.GroupPermissionValidator;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroupService.
 * Tests all group operations including creation, member management, and retrieval.
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupPermissionValidator permissionValidator;

    private GroupService groupService;

    private User creator;
    private User member;
    private Group group;
    private GroupMember adminMember;
    private GroupMember regularMember;

    @BeforeEach
    void setUp() {
        groupService = new GroupServiceImpl(
            groupRepository,
            groupMemberRepository,
            userRepository,
            permissionValidator
        );

        // Setup test users
        creator = new User("creator@test.com", "password", "Creator", "ACTIVE", List.of("ROLE_USER"));
        creator.setId(1L);

        member = new User("member@test.com", "password", "Member", "ACTIVE", List.of("ROLE_USER"));
        member.setId(2L);

        // Setup test group
        group = new Group();
        group.setId(100L);
        group.setName("Test Group");
        group.setDescription("A test group");
        group.setCreatedBy(creator);
        group.setCreatedAt(LocalDateTime.now());

        // Setup group members
        adminMember = new GroupMember();
        adminMember.setId(1L);
        adminMember.setGroup(group);
        adminMember.setUser(creator);
        adminMember.setRole(GroupRole.ADMIN);
        adminMember.setJoinedAt(LocalDateTime.now());

        regularMember = new GroupMember();
        regularMember.setId(2L);
        regularMember.setGroup(group);
        regularMember.setUser(member);
        regularMember.setRole(GroupRole.MEMBER);
        regularMember.setJoinedAt(LocalDateTime.now());

        group.setMembers(List.of(adminMember, regularMember));
    }

    // ========================================================================
    // CREATE GROUP TESTS
    // ========================================================================

    @Nested
    @DisplayName("Create Group")
    class CreateGroupTests {

        @Test
        @DisplayName("Successfully creates group with creator as admin")
        void createGroup_Success() {
            // Arrange
            GroupCreateRequest request = new GroupCreateRequest();
            request.setName("New Group");
            request.setDescription("Description");

            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
                Group saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            // Act
            GroupResponse response = groupService.createGroup(1L, request);

            // Assert
            assertNotNull(response);
            assertEquals("New Group", response.getName());
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("Throws exception for empty group name")
        void createGroup_EmptyName_ThrowsException() {
            // Arrange
            GroupCreateRequest request = new GroupCreateRequest();
            request.setName("");

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> groupService.createGroup(1L, request));
            verify(groupRepository, never()).save(any());
        }
    }

    // ========================================================================
    // GET GROUP TESTS
    // ========================================================================

    @Nested
    @DisplayName("Get Group")
    class GetGroupTests {

        @Test
        @DisplayName("Successfully gets group as member")
        void getGroup_AsMember_Success() {
            // Arrange
            when(permissionValidator.validateMember(100L, 2L)).thenReturn(group);
            when(groupRepository.findById(100L)).thenReturn(Optional.of(group));

            // Act
            GroupResponse response = groupService.getGroup(100L, 2L);

            // Assert
            assertNotNull(response);
            assertEquals(100L, response.getId());
            assertEquals("Test Group", response.getName());
        }

        @Test
        @DisplayName("Throws exception for non-member")
        void getGroup_NotMember_ThrowsException() {
            // Arrange
            when(permissionValidator.validateMember(100L, 3L))
                .thenThrow(new GroupAccessDeniedException("Not a member"));

            // Act & Assert
            assertThrows(GroupAccessDeniedException.class,
                () -> groupService.getGroup(100L, 3L));
        }

        @Test
        @DisplayName("Successfully gets user's groups")
        void getUserGroups_Success() {
            // Arrange
            when(groupRepository.findGroupsByMemberId(2L)).thenReturn(List.of(group));

            // Act
            List<GroupResponse> responses = groupService.getUserGroups(2L);

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(100L, responses.get(0).getId());
        }
    }

    // ========================================================================
    // UPDATE GROUP TESTS
    // ========================================================================

    @Nested
    @DisplayName("Update Group")
    class UpdateGroupTests {

        @Test
        @DisplayName("Successfully updates group as admin")
        void updateGroup_AsAdmin_Success() {
            // Arrange - validateAdmin returns the group directly
            when(permissionValidator.validateAdmin(100L, 1L)).thenReturn(group);
            when(groupRepository.save(any(Group.class))).thenReturn(group);

            // Act
            GroupResponse response = groupService.updateGroup(100L, 1L, "Updated Name", "Updated Desc");

            // Assert
            assertNotNull(response);
            verify(groupRepository).save(any(Group.class));
        }

        @Test
        @DisplayName("Throws exception for non-admin")
        void updateGroup_NotAdmin_ThrowsException() {
            // Arrange
            when(permissionValidator.validateAdmin(100L, 2L))
                .thenThrow(new GroupAccessDeniedException("Not an admin"));

            // Act & Assert
            assertThrows(GroupAccessDeniedException.class,
                () -> groupService.updateGroup(100L, 2L, "New Name", "New Desc"));
        }
    }

    // ========================================================================
    // REMOVE MEMBER TESTS
    // ========================================================================

    @Nested
    @DisplayName("Remove Member")
    class RemoveMemberTests {

        @Test
        @DisplayName("Successfully removes member as admin")
        void removeMember_AsAdmin_Success() {
            // Arrange
            when(permissionValidator.validateAdmin(100L, 1L)).thenReturn(group);
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(regularMember));

            // Act
            groupService.removeMember(100L, 1L, 2L);

            // Assert
            verify(groupMemberRepository).delete(regularMember);
        }

        @Test
        @DisplayName("Throws exception when removing last admin")
        void removeMember_LastAdmin_ThrowsException() {
            // Arrange
            when(permissionValidator.validateAdmin(100L, 1L)).thenReturn(group);
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 3L))
                .thenReturn(Optional.of(adminMember));  // Target is also admin
            when(groupMemberRepository.countAdminsByGroupId(100L)).thenReturn(1L);

            // Act & Assert
            assertThrows(RuntimeException.class,
                () -> groupService.removeMember(100L, 1L, 3L));
        }

        @Test
        @DisplayName("Throws exception when non-admin tries to remove")
        void removeMember_NotAdmin_ThrowsException() {
            // Arrange
            when(permissionValidator.validateAdmin(100L, 2L))
                .thenThrow(new GroupAccessDeniedException("Not an admin"));

            // Act & Assert
            assertThrows(GroupAccessDeniedException.class,
                () -> groupService.removeMember(100L, 2L, 3L));
        }
    }

    // ========================================================================
    // PROMOTE MEMBER TESTS
    // ========================================================================

    @Nested
    @DisplayName("Promote Member")
    class PromoteMemberTests {

        @Test
        @DisplayName("Successfully promotes member to admin")
        void promoteMember_Success() {
            // Arrange
            when(permissionValidator.validateAdmin(100L, 1L)).thenReturn(group);
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(regularMember));
            when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> {
                GroupMember saved = invocation.getArgument(0);
                saved.setRole(GroupRole.ADMIN);  // Simulate the role change
                return saved;
            });

            // Act
            GroupMemberResponse response = groupService.promoteMember(100L, 1L, 2L);

            // Assert
            assertNotNull(response);
            verify(groupMemberRepository).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("Throws exception when promoting non-member")
        void promoteMember_NotMember_ThrowsException() {
            // Arrange
            when(permissionValidator.validateAdmin(100L, 1L)).thenReturn(group);
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 3L))
                .thenReturn(Optional.empty());

            // Act & Assert (service throws RuntimeException, not GroupNotFoundException)
            assertThrows(RuntimeException.class,
                () -> groupService.promoteMember(100L, 1L, 3L));
        }
    }

    // ========================================================================
    // LEAVE GROUP TESTS
    // ========================================================================

    @Nested
    @DisplayName("Leave Group")
    class LeaveGroupTests {

        @Test
        @DisplayName("Successfully leaves group as member")
        void leaveGroup_AsMember_Success() {
            // Arrange
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(regularMember));

            // Act
            groupService.leaveGroup(100L, 2L);

            // Assert
            verify(groupMemberRepository).delete(regularMember);
        }

        @Test
        @DisplayName("Throws exception when last admin tries to leave with other members")
        void leaveGroup_LastAdmin_ThrowsException() {
            // Arrange
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(adminMember));
            when(groupMemberRepository.countAdminsByGroupId(100L)).thenReturn(1L);
            when(groupMemberRepository.countByGroupId(100L)).thenReturn(2L);

            // Act & Assert
            assertThrows(RuntimeException.class,
                () -> groupService.leaveGroup(100L, 1L));
            verify(groupMemberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Admin can leave when other admins exist")
        void leaveGroup_AdminWithOtherAdmins_Success() {
            // Arrange
            when(groupMemberRepository.findByGroupIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(adminMember));
            when(groupMemberRepository.countAdminsByGroupId(100L)).thenReturn(2L);

            // Act
            groupService.leaveGroup(100L, 1L);

            // Assert
            verify(groupMemberRepository).delete(adminMember);
        }
    }

    // ========================================================================
    // GET GROUP MEMBERS TESTS
    // ========================================================================

    @Nested
    @DisplayName("Get Group Members")
    class GetGroupMembersTests {

        @Test
        @DisplayName("Successfully gets group members")
        void getGroupMembers_Success() {
            // Arrange
            when(permissionValidator.validateMember(100L, 2L)).thenReturn(group);
            when(groupMemberRepository.findByGroupId(100L))
                .thenReturn(List.of(adminMember, regularMember));

            // Act
            List<GroupMemberResponse> responses = groupService.getGroupMembers(100L, 2L);

            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size());
        }

        @Test
        @DisplayName("Throws exception for non-member")
        void getGroupMembers_NotMember_ThrowsException() {
            // Arrange
            when(permissionValidator.validateMember(100L, 3L))
                .thenThrow(new GroupAccessDeniedException("Not a member"));

            // Act & Assert
            assertThrows(GroupAccessDeniedException.class,
                () -> groupService.getGroupMembers(100L, 3L));
        }
    }
}
