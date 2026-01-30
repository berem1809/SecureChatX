package com.chatapp.dto;

/**
 * DTO for uploading public key
 * Request body when user sends their public key to server
 */
public class PublicKeyUploadRequest {

    private String publicKey;

    public PublicKeyUploadRequest() {}

    public PublicKeyUploadRequest(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}
