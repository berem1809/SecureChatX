package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupKeyResponse {
    private String key;
    private String encryptedKey;
    private String nonce;
    private String senderPublicKey;

    public GroupKeyResponse(String key) {
        this.key = key;
    }
}
