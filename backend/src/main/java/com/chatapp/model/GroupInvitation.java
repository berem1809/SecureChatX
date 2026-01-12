package com.chatapp.model;

// ============================================================================
// IMPORTS
// ============================================================================

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * GROUP INVITATION ENTITY - Represents an invitation to join a group
 * ============================================================================
 * 
 * WHAT IS A GROUP INVITATION?
 * ---------------------------
 * When a group admin wants to add a new member, they send an invitation.
 * The invited user can accept or reject the invitation.
 * 
 * INVITATION WORKFLOW:
 * --------------------
 * 1. Admin invites User X → Invitation created with PENDING status
 * 2. User X accepts → Status: ACCEPTED → GroupMember created
 *    OR
 * 2. User X rejects → Status: REJECTED → No membership created
 * 
 * DATABASE TABLE:
 * ---------------
 * CREATE TABLE group_invitations (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     group_id BIGINT NOT NULL REFERENCES groups(id),
 *     inviter_id BIGINT NOT NULL REFERENCES users(id),
 *     invitee_id BIGINT NOT NULL REFERENCES users(id),
 *     status VARCHAR(20) NOT NULL,
 *     created_at TIMESTAMP NOT NULL,
 *     updated_at TIMESTAMP,
 *     UNIQUE(group_id, invitee_id)  -- One pending invitation per user per group
 * );
 * 
 * @see GroupInvitationService For business logic
 * @see GroupController For REST endpoints
 */
@Entity
@Table(
    name = "group_invitations",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"group_id", "invitee_id"},
        name = "uk_group_invitation"
    ),
    indexes = {
        @Index(name = "idx_ginv_group", columnList = "group_id"),
        @Index(name = "idx_ginv_invitee", columnList = "invitee_id"),
        @Index(name = "idx_ginv_status", columnList = "status")
    }
)
public class GroupInvitation {

    // ========================================================================
    // PRIMARY KEY
    // ========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // RELATIONSHIPS
    // ========================================================================

    /**
     * The group the invitation is for.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    /**
     * The admin who sent the invitation.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    /**
     * The user being invited to join the group.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    // ========================================================================
    // STATUS
    // ========================================================================

    /**
     * Current status of the invitation.
     * Values: PENDING, ACCEPTED, REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupInvitationStatus status = GroupInvitationStatus.PENDING;

    // ========================================================================
    // TIMESTAMPS
    // ========================================================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public GroupInvitation() {}

    public GroupInvitation(Group group, User inviter, User invitee) {
        this.group = group;
        this.inviter = inviter;
        this.invitee = invitee;
        this.status = GroupInvitationStatus.PENDING;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public User getInviter() { return inviter; }
    public void setInviter(User inviter) { this.inviter = inviter; }

    public User getInvitee() { return invitee; }
    public void setInvitee(User invitee) { this.invitee = invitee; }

    public GroupInvitationStatus getStatus() { return status; }
    public void setStatus(GroupInvitationStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
