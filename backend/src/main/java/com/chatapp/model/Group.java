package com.chatapp.model;

// ============================================================================
// IMPORTS
// ============================================================================

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * GROUP ENTITY - Represents a group chat
 * ============================================================================
 * 
 * WHAT IS A GROUP?
 * ----------------
 * A group is a multi-user chat channel. Unlike one-to-one conversations:
 * - Groups can have many members (no limit defined yet)
 * - Groups have names and descriptions
 * - Groups have role-based access (ADMIN, MEMBER)
 * - Members are added through invitations
 * 
 * GROUP CREATION WORKFLOW:
 * ------------------------
 * 1. User creates a group → User becomes ADMIN
 * 2. Admin invites other users → Invitations created with PENDING status
 * 3. Invited users accept → They become MEMBERs
 * 
 * DATABASE TABLE:
 * ---------------
 * CREATE TABLE `groups` (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     name VARCHAR(100) NOT NULL,
 *     description VARCHAR(500),
 *     created_by BIGINT NOT NULL REFERENCES users(id),
 *     created_at TIMESTAMP NOT NULL,
 *     updated_at TIMESTAMP,
 *     last_message_at TIMESTAMP
 * );
 * 
 * @see GroupMember For group membership
 * @see GroupInvitation For invitation workflow
 * @see GroupService For business logic
 */
@Entity
@Table(
    name = "`groups`",  // 'groups' is a reserved word in some databases
    indexes = {
        @Index(name = "idx_group_created_by", columnList = "created_by"),
        @Index(name = "idx_group_last_message", columnList = "last_message_at")
    }
)
public class Group {

    // ========================================================================
    // PRIMARY KEY
    // ========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // GROUP METADATA
    // ========================================================================

    /**
     * Name of the group (visible to all members).
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Optional description of the group purpose.
     */
    @Column(length = 500)
    private String description;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    /**
     * The user who created the group.
     * This user automatically becomes an ADMIN.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * All members of this group.
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    /**
     * All pending invitations for this group.
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupInvitation> invitations = new ArrayList<>();

    // ========================================================================
    // TIMESTAMPS
    // ========================================================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Timestamp of the last message in this group.
     * Used for sorting groups by recent activity.
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    // ========================================================================
    // JPA LIFECYCLE CALLBACKS
    // ========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public Group() {}

    public Group(String name, String description, User createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public List<GroupMember> getMembers() { return members; }
    public void setMembers(List<GroupMember> members) { this.members = members; }

    public List<GroupInvitation> getInvitations() { return invitations; }
    public void setInvitations(List<GroupInvitation> invitations) { this.invitations = invitations; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
