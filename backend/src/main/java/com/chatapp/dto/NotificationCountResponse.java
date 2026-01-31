package com.chatapp.dto;

/**
 * ============================================================================
 * NOTIFICATION COUNT RESPONSE DTO
 * ============================================================================
 * 
 * Response object containing notification counts for a user.
 * Used to display badge counts in the navigation bar.
 */
public class NotificationCountResponse {

    private long unreadDirectMessages;
    private long unreadGroupMessages;
    private long pendingChatRequests;
    private long pendingGroupInvitations;
    private long totalNotifications;

    public NotificationCountResponse() {}

    public NotificationCountResponse(long unreadDirectMessages, long unreadGroupMessages, 
                                     long pendingChatRequests, long pendingGroupInvitations) {
        this.unreadDirectMessages = unreadDirectMessages;
        this.unreadGroupMessages = unreadGroupMessages;
        this.pendingChatRequests = pendingChatRequests;
        this.pendingGroupInvitations = pendingGroupInvitations;
        this.totalNotifications = unreadDirectMessages + unreadGroupMessages + 
                                  pendingChatRequests + pendingGroupInvitations;
    }

    // Getters and Setters
    public long getUnreadDirectMessages() { return unreadDirectMessages; }
    public void setUnreadDirectMessages(long unreadDirectMessages) { 
        this.unreadDirectMessages = unreadDirectMessages;
        updateTotal();
    }

    public long getUnreadGroupMessages() { return unreadGroupMessages; }
    public void setUnreadGroupMessages(long unreadGroupMessages) { 
        this.unreadGroupMessages = unreadGroupMessages;
        updateTotal();
    }

    public long getPendingChatRequests() { return pendingChatRequests; }
    public void setPendingChatRequests(long pendingChatRequests) { 
        this.pendingChatRequests = pendingChatRequests;
        updateTotal();
    }

    public long getPendingGroupInvitations() { return pendingGroupInvitations; }
    public void setPendingGroupInvitations(long pendingGroupInvitations) { 
        this.pendingGroupInvitations = pendingGroupInvitations;
        updateTotal();
    }

    public long getTotalNotifications() { return totalNotifications; }

    private void updateTotal() {
        this.totalNotifications = unreadDirectMessages + unreadGroupMessages + 
                                  pendingChatRequests + pendingGroupInvitations;
    }
}
