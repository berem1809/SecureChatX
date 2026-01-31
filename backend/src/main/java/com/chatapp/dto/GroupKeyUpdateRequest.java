package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a member's wrapped group key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupKeyUpdateRequest {
    private String encryptedKey;
    private String nonce;
    private String senderPublicKey;
}
