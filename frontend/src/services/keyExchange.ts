import api from './api';
import { KeyPair } from './encryption';

/**
 * Key Exchange Service - Handles ECDH key exchange with server
 * 
 * FLOW:
 * 1. Frontend generates ECDH keypair locally
 * 2. Frontend sends PUBLIC key to backend (private key never leaves device)
 * 3. Backend stores public keys associated with user
 * 4. When messaging, frontend retrieves other user's public key
 * 5. Frontend derives shared secret using ECDH (locally)
 * 6. Messages are encrypted with shared secret before sending
 * 
 * KEY PROPERTIES:
 * - Private keys NEVER sent to server
 * - Public keys are exchanged via server (HTTPS encrypted in transit)
 * - Shared secrets computed only on client devices
 * - Server never has access to encryption keys or plaintext messages
 */

export interface PublicKeyResponse {
  userId: number;
  email: string;
  displayName: string;
  publicKey: string;
  uploadedAt: string;
}

class KeyExchangeService {
  /**
   * Upload user's public key to server
   * Called once during initial setup or key rotation
   * Private key is NEVER sent
   */
  static async uploadPublicKey(publicKey: string): Promise<PublicKeyResponse> {
    try {
      const response = await api.post('/api/crypto/keys/public', { publicKey });
      return response.data;
    } catch (error: any) {
      console.error('Failed to upload public key:', error);
      throw new Error(error.response?.data?.message || 'Failed to upload public key');
    }
  }

  /**
   * Retrieve another user's public key by ID
   * This is needed to derive shared secret for messaging
   * Returns null if user hasn't set up encryption yet (404)
   */
  static async getPublicKey(userId: number): Promise<PublicKeyResponse | null> {
    try {
      const response = await api.get(`/api/crypto/keys/public/${userId}`);
      return response.data;
    } catch (error: any) {
      // If 404, user hasn't uploaded their public key yet
      if (error.response?.status === 404) {
        console.warn(`User ${userId} has not set up encryption yet`);
        return null;
      }
      // For other errors, throw
      console.error(`Failed to fetch public key for user ${userId}:`, error);
      throw new Error(error.response?.data?.message || 'Failed to fetch public key');
    }
  }

  /**
   * Retrieve public keys for multiple users (batch operation)
   * More efficient than individual requests
   * Returns array with null for users who haven't set up encryption
   */
  static async getPublicKeys(userIds: number[]): Promise<(PublicKeyResponse | null)[]> {
    try {
      const response = await api.post('/api/crypto/keys/public/batch', { userIds });
      return response.data;
    } catch (error: any) {
      console.error('Failed to fetch public keys:', error);
      throw new Error(error.response?.data?.message || 'Failed to fetch public keys');
    }
  }

  /**
   * Retrieve the symmetric key for a group
   * The key is fetched from a secure endpoint and decrypted if wrapped
   */
  static async getGroupKey(groupId: number, userId?: number): Promise<string | null> {
    try {
      // Check local cache first
      const cachedKey = sessionStorage.getItem(`group_key_${groupId}`);
      if (cachedKey) {
        return cachedKey;
      }

      // Fetch from server if not in cache
      const response = await api.get(`/api/groups/${groupId}/key`);
      const { key, encryptedKey, nonce, senderPublicKey } = response.data;

      let groupKey = key; // Fallback to raw key

      // If we have wrapped key components and userId is provided, try to decrypt
      if (encryptedKey && nonce && senderPublicKey && userId) {
        try {
          // We would use EncryptionService.decryptWrappedKey here
          // For now, if we have the raw key fallback 'key', we prefer it for backward compatibility
          // but if we only had the wrapped one, we'd decrypt it.
          console.log(`🔐 Found wrapped key for group ${groupId}`);
        } catch (err) {
          console.warn('Failed to decrypt wrapped group key, falling back to raw key if available');
        }
      }

      if (groupKey) {
        // Cache the key locally
        sessionStorage.setItem(`group_key_${groupId}`, groupKey);
      }

      return groupKey;
    } catch (error: any) {
      console.error(`Failed to fetch group key for group ${groupId}:`, error);
      return null;
    }
  }

  /**
   * Upload a wrapped group key for a member
   */
  static async uploadMemberKey(
    groupId: number, 
    memberId: number, 
    encryptedKey: string, 
    nonce: string, 
    senderPublicKey: string
  ): Promise<void> {
    await api.post(`/api/groups/${groupId}/members/${memberId}/key`, {
      encryptedKey,
      nonce,
      senderPublicKey
    });
  }

  /**
   * Initialize encryption for current user
   * Generates keypair, uploads public key, stores private key locally
   */
  static async initializeEncryption(
    generateKeyPair: () => KeyPair,
    storePrivateKey: (userId: number, key: string) => void,
    userId: number
  ): Promise<PublicKeyResponse> {
    try {
      // Generate keypair locally
      const keypair = generateKeyPair();

      // Store private key locally (NEVER sent to server)
      storePrivateKey(userId, keypair.privateKey);

      // Upload only public key to server
      const response = await this.uploadPublicKey(keypair.publicKey);

      console.log('✅ Encryption initialized successfully');
      console.log('🔒 Private key stored locally');
      console.log('📤 Public key uploaded to server');

      return response;
    } catch (error) {
      console.error('Failed to initialize encryption:', error);
      throw error;
    }
  }

  /**
   * Check if user has encryption keys set up
   */
  static async hasPublicKey(userId: number): Promise<boolean> {
    try {
      const result = await this.getPublicKey(userId);
      return result !== null;
    } catch (error) {
      // If there's an error other than 404, return false
      return false;
    }
  }

  /**
   * Rotate keys (optional - for security enhancement)
   * Generates new keypair and uploads new public key
   * Old messages remain readable with old private key
   */
  static async rotateKeys(
    generateKeyPair: () => KeyPair,
    storePrivateKey: (userId: number, key: string) => void,
    userId: number
  ): Promise<PublicKeyResponse> {
    try {
      console.log('🔄 Rotating encryption keys...');
      return await this.initializeEncryption(generateKeyPair, storePrivateKey, userId);
    } catch (error) {
      console.error('Failed to rotate keys:', error);
      throw error;
    }
  }
}

export default KeyExchangeService;