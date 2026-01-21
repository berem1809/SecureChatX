package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ============================================================================
 * GROUP CREATE REQUEST DTO
 * ============================================================================
 * 
 * Request object for creating a new group chat.
 * 
 * NOTE: The creator is determined from the JWT token, not from this request.
 * The creator automatically becomes the group admin.
 */
public class GroupCreateRequest {

    /**
     * Name of the group (required).
     */
    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 100, message = "Group name must be between 1 and 100 characters")
    private String name;

    /**
     * Mandatory description of the group purpose.
     * This helps members understand what the group is for.
     */
    @NotBlank(message = "Group description is required")
    @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
    private String description;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    public GroupCreateRequest() {}

    public GroupCreateRequest(String name) {
        this.name = name;
    }

    public GroupCreateRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
