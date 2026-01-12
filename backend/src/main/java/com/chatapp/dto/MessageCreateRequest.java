package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for sending a new message.
 */
public class MessageCreateRequest {

    @NotBlank(message = "Message content cannot be empty")
    private String content;

    public MessageCreateRequest() {}

    public MessageCreateRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
