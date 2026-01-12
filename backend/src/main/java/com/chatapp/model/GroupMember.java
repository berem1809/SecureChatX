package com.chatapp.model;

// ============================================================================
// IMPORTS
// ============================================================================

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * GROUP MEMBER ENTITY - Links users to groups with roles
 * ============================================================================
 * 
 * WHAT IS A GROUP MEMBER?
 * -----------------------
 * This entity represents a user's membership in a group, including:
 * - Their role (ADMIN or MEMBER)
 * - When they joined
 * - Notification settings
 * 
 * ROLE HIERARCHY:
 * ---------------
 * ADMIN - Can:
 *   - Invite new members
 *   - Remove members
 *   - Promote members to admin
 *   - Delete the group (if creator)
 *   - All MEMBER permissions
 * 
 * MEMBER - Can:
 *   - Send messages
 *   - Read messages
 *   - Leave the group
 * 
 * DATABASE TABLE:
 * ---------------
 * CREATE TABLE group_members (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     group_id BIGINT NOT NULL REFERENCES groups(id),
 *     user_id BIGINT NOT NULL REFERENCES users(id),
 *     role VARCHAR(20) NOT NULL,
 *     joined_at TIMESTAMP NOT NULL,
 *     last_read_at TIMESTAMP,
 *     is_muted BOOLEAN DEFAULT FALSE,
 *     UNIQUE(group_id, user_id)
 * );
 */
@Entity
@Table(
    name = "group_members",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"group_id", "user_id"},
        name = "uk_group_member"
    ),
    indexes = {
        @Index(name = "idx_group_member_user", columnList = "user_id"),
        @Index(name = "idx_group_member_group", columnList = "group_id"),
        @Index(name = "idx_group_member_role", columnList = "role")
    }
)
public class GroupMember {

    // ========================================================================
    // PRIMARY KEY
    // ========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ========================================================================
    // MEMBER ATTRIBUTES
    // ========================================================================

    /**
     * The member's role in the group.
     * Values: ADMIN, MEMBER
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role = GroupRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * Timestamp of when user last read messages in this group.
     * Used for unread message count.
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /**
     * Whether the user has muted notifications for this group.
     */
    @Column(name = "is_muted")
    private boolean muted = false;

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public GroupMember() {}

    public GroupMember(Group group, User user, GroupRole role) {
        this.group = group;
        this.user = user;
        this.role = role;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Checks if this member is an admin.
     */
    public boolean isAdmin() {
        return role == GroupRole.ADMIN;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public GroupRole getRole() { return role; }
    public void setRole(GroupRole role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }

    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
}
