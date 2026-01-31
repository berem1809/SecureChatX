package com.chatapp.model;

// ============================================================================
// IMPORTS
// ============================================================================

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.chatapp.util.EncryptionUtil;

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
     * Description of the group (mandatory).
     * Helps members understand the purpose of the group.
     */
    @Column(nullable = false, length = 500)
    private String description;

    /**
     * Symmetric key for group E2EE.
     * Encrypted and stored securely.
     * Nullable for existing groups - key will be generated on first access.
     */
    @Column(name = "group_key", length = 255)
    private String groupKey;

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
     * The last message sent in this group.
     * Used for displaying previews in the group list.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private Message lastMessage;

    // ========================================================================
    // TIMESTAMPS
    // ========================================================================

    /**
     * When the group was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the group was last updated (e.g., name change).
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Timestamp of the last message sent in the group.
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

    public Group() {
        this.createdAt = LocalDateTime.now();
        this.groupKey = EncryptionUtil.generateSecureKey(32); // Generate a 256-bit key
    }

    public Group(String name, String description, User createdBy) {
        this(); // Call default constructor to set createdAt and generate key
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public List<GroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
}
