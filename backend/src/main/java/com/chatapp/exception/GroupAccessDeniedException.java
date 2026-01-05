package com.chatapp.exception;

/**
 * ============================================================================
 * GROUP ACCESS DENIED EXCEPTION
 * ============================================================================
 * 
 * Thrown when a user attempts to access a group they are not a member of,
 * or when a non-admin tries to perform admin-only operations.
 * Results in HTTP 403 Forbidden response.
 * 
 * EXAMPLE SCENARIOS:
 * ------------------
 * - User tries to access a group they're not a member of
 * - Non-admin tries to invite users to a group
 * - Non-admin tries to remove a member from a group
 * - Non-admin tries to edit group information
 */
public class GroupAccessDeniedException extends RuntimeException {

    public GroupAccessDeniedException(String message) {
        super(message);
    }

    public GroupAccessDeniedException(Long groupId, Long userId) {
        super(String.format("User %d does not have access to group %d", userId, groupId));
    }

    /**
     * Creates an exception for insufficient role permissions.
     * 
     * @param groupId The group ID
     * @param userId The user ID
     * @param requiredRole The required role (e.g., "ADMIN")
     * @return GroupAccessDeniedException with role-specific message
     */
    public static GroupAccessDeniedException insufficientRole(Long groupId, Long userId, String requiredRole) {
        return new GroupAccessDeniedException(
            String.format("User %d does not have %s role in group %d", userId, requiredRole, groupId)
        );
    }
}
