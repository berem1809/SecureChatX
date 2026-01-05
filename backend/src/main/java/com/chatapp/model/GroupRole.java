package com.chatapp.model;

/**
 * ============================================================================
 * GROUP ROLE ENUM
 * ============================================================================
 * 
 * Defines the possible roles a user can have in a group.
 * 
 * PERMISSION MATRIX:
 * ------------------
 * | Action              | ADMIN | MEMBER |
 * |---------------------|-------|--------|
 * | Send messages       |  ✓    |   ✓    |
 * | Read messages       |  ✓    |   ✓    |
 * | Leave group         |  ✓    |   ✓    |
 * | Invite members      |  ✓    |   ✗    |
 * | Remove members      |  ✓    |   ✗    |
 * | Promote to admin    |  ✓    |   ✗    |
 * | Edit group info     |  ✓    |   ✗    |
 */
public enum GroupRole {
    /**
     * Admin role with full permissions.
     * The group creator is automatically an admin.
     * Admins can promote other members to admin.
     */
    ADMIN,
    
    /**
     * Regular member role with basic permissions.
     * Members can send/read messages and leave the group.
     */
    MEMBER
}
