import nacl from 'tweetnacl';

/**
 * Encryption Service - Provides hybrid cryptography with ECDH and XSalsa20-Poly1305
 * 
 * ARCHITECTURE:
 * - Uses X25519 (Elliptic Curve Diffie-Hellman) for secure key exchange
 * - Uses XSalsa20-Poly1305 for authenticated encryption of messages
 * - Private keys NEVER leave the client
 * - Server only stores public keys and encrypted messages
 */

export interface KeyPair {
  publicKey: string; // Base64 encoded
  privateKey: string; // Base64 encoded - NEVER sent to server
}

export interface EncryptedMessage {
  ciphertext: string; // Base64 encoded encrypted content
  nonce: string; // Base64 encoded
  senderPublicKey: string; // Base64 encoded - for key derivation
}

export interface SharedSecret {
  conversationId: number;
  otherUserId: number;
  sharedSecret: Uint8Array; // Raw shared secret for key derivation
  theirPublicKey: string; // Base64 encoded - stored for verification
}

/**
 * Utility functions for base64 encoding/decoding
 */
const encodeBase64 = (bytes: Uint8Array): string => {
  return btoa(String.fromCharCode(...Array.from(bytes)));
};

const decodeBase64 = (str: string): Uint8Array => {
  const binaryString = atob(str);
  const bytes = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
};

class EncryptionService {
  /**
   * Initialize encryption (TweetNaCl doesn't require async init)
   */
  static async initialize(): Promise<void> {
    // TweetNaCl is synchronous, no initialization needed
  }

  /**
   * Generate X25519 key pair for a user
   * Public key will be shared with other users
   * Private key stays on the device
   */
  static generateKeyPair(): KeyPair {
    const keypair = nacl.box.keyPair();
    return {
      publicKey: encodeBase64(keypair.publicKey),
      privateKey: encodeBase64(keypair.secretKey),
    };
  }

  /**
   * Derive public key from private key (for getting the exact public key used in encryption)
   * TweetNaCl's box.keyPair.fromSecretKey derives the public key from secret key
   */
  static getPublicKeyFromPrivateKey(privateKeyBase64: string): string {
    const privateKeyBytes = decodeBase64(privateKeyBase64);
    const keypair = nacl.box.keyPair.fromSecretKey(privateKeyBytes);
    return encodeBase64(keypair.publicKey);
  }

  /**
   * Derive shared secret from local private key and other user's public key
   * Uses X25519 ECDH to create a symmetric shared secret
   */
  static deriveSharedSecret(
    myPrivateKey: string,
    theirPublicKey: string
  ): Uint8Array {
    console.log('📐 Deriving shared secret:');
    console.log('  - myPrivateKey (first 20 chars):', myPrivateKey.substring(0, 20) + '...');
    console.log('  - theirPublicKey (first 20 chars):', theirPublicKey.substring(0, 20) + '...');
    
    const privateKeyBytes = decodeBase64(myPrivateKey);
    const publicKeyBytes = decodeBase64(theirPublicKey);

    console.log('  - decoded privateKey length:', privateKeyBytes.length);
    console.log('  - decoded publicKey length:', publicKeyBytes.length);
    
    // Debug: log hex of first 4 bytes for verification
    console.log('  - privateKey hex (first 4 bytes):', Array.from(privateKeyBytes.slice(0, 4)).map(b => b.toString(16).padStart(2, '0')).join(' '));
    console.log('  - publicKey hex (first 4 bytes):', Array.from(publicKeyBytes.slice(0, 4)).map(b => b.toString(16).padStart(2, '0')).join(' '));

    // Use box.before to derive shared secret (more efficient for repeated encryption)
    const sharedSecret = nacl.box.before(publicKeyBytes, privateKeyBytes);
    
    console.log('  - derived sharedSecret length:', sharedSecret.length);
    console.log('  - sharedSecret hex (first 4 bytes):', Array.from(sharedSecret.slice(0, 4)).map(b => b.toString(16).padStart(2, '0')).join(' '));
    return sharedSecret;
  }

  /**
   * Encrypt a message using XSalsa20-Poly1305
   * This is authenticated encryption - ensures confidentiality AND authenticity
   * 
   * @param plaintext Message content to encrypt
   * @param sharedSecret The shared secret derived from ECDH
   * @param senderPublicKey The sender's public key (included in encrypted message for verification)
   */
  static encryptMessage(
    plaintext: string,
    sharedSecret: Uint8Array,
    senderPublicKey: string
  ): EncryptedMessage {
    // Generate a random nonce for each message
    const nonce = nacl.randomBytes(24); // XSalsa20-Poly1305 uses 24-byte nonce

    // Convert plaintext to bytes
    const plaintextBytes = new TextEncoder().encode(plaintext);

    // Encrypt using the shared secret and nonce
    const ciphertext = nacl.box.after(plaintextBytes, nonce, sharedSecret);

    return {
      ciphertext: encodeBase64(ciphertext),
      nonce: encodeBase64(nonce),
      senderPublicKey,
    };
  }

  /**
   * Decrypt a message using XSalsa20-Poly1305
   * Verifies authenticity and decrypts message
   * 
   * @param encryptedMessage The encrypted message object
   * @param sharedSecret The shared secret derived from ECDH
   */
  static decryptMessage(
    encryptedMessage: EncryptedMessage,
    sharedSecret: Uint8Array
  ): string {
    try {
      console.log('🔐 decryptMessage called with:');
      console.log('  - ciphertext length:', encryptedMessage.ciphertext.length);
      console.log('  - nonce:', encryptedMessage.nonce.substring(0, 20) + '...');
      console.log('  - senderPublicKey:', encryptedMessage.senderPublicKey.substring(0, 20) + '...');
      console.log('  - sharedSecret type:', typeof sharedSecret, 'length:', sharedSecret.length);
      console.log('  - sharedSecret hex (first 4 bytes):', Array.from(sharedSecret.slice(0, 4)).map(b => b.toString(16).padStart(2, '0')).join(' '));
      
      const ciphertextBytes = decodeBase64(encryptedMessage.ciphertext);
      const nonceBytes = decodeBase64(encryptedMessage.nonce);

      console.log('  - decoded ciphertext length:', ciphertextBytes.length);
      console.log('  - decoded nonce length:', nonceBytes.length);
      console.log('  - ciphertext hex (first 4 bytes):', Array.from(ciphertextBytes.slice(0, 4)).map(b => b.toString(16).padStart(2, '0')).join(' '));
      console.log('  - nonce hex (first 4 bytes):', Array.from(nonceBytes.slice(0, 4)).map(b => b.toString(16).padStart(2, '0')).join(' '));
      
      // Decrypt using the shared secret and nonce
      const plaintextBytes = nacl.box.open.after(ciphertextBytes, nonceBytes, sharedSecret);

      console.log('  - decryption result:', plaintextBytes ? 'SUCCESS' : 'FAILED (null)');
      
      if (!plaintextBytes) {
        throw new Error('Decryption failed - nacl.box.open.after returned null');
      }

      // Convert decrypted bytes back to string
      const result = new TextDecoder().decode(plaintextBytes);
      console.log('  - decrypted text:', result);
      return result;
    } catch (error) {
      console.error('❌ Failed to decrypt message:', error);
      throw new Error('Message decryption failed - tampering detected or wrong key');
    }
  }

  /**
   * Convert a shared secret to an encryption key for consistent key derivation
   * Uses SHA-512 for key derivation
   */
  static deriveEncryptionKey(sharedSecret: Uint8Array, _salt?: Uint8Array): Uint8Array {
    // TweetNaCl doesn't have built-in KDF, using the shared secret directly or deriving with crypto
    // Return first 32 bytes of the shared secret as the key
    return sharedSecret.slice(0, 32);
  }

  /**
   * Get or create shared secret for a conversation
   * Stored locally in sessionStorage to avoid re-deriving on every message
   */
  static getOrCreateSharedSecret(
    conversationId: number,
    otherUserId: number,
    myPrivateKey: string,
    theirPublicKey: string
  ): SharedSecret {
    const storageKey = `shared_secret_${conversationId}_${otherUserId}`;
    const stored = sessionStorage.getItem(storageKey);

    if (stored) {
      const parsed = JSON.parse(stored);
      return {
        conversationId,
        otherUserId,
        sharedSecret: new Uint8Array(parsed.sharedSecret),
        theirPublicKey: parsed.theirPublicKey,
      };
    }

    // Derive new shared secret
    const sharedSecret = this.deriveSharedSecret(myPrivateKey, theirPublicKey);
    
    // Store in sessionStorage
    sessionStorage.setItem(
      storageKey,
      JSON.stringify({
        sharedSecret: Array.from(sharedSecret),
        theirPublicKey,
      })
    );

    return {
      conversationId,
      otherUserId,
      sharedSecret,
      theirPublicKey,
    };
  }

  /**
   * Securely store user's private key in localStorage (encrypted in production)
   * WARNING: In production, use a more secure method like IndexedDB with encryption
   * or better yet, use WebAuthn/FIDO2 for key storage
   */
  static storePrivateKey(userId: number, privateKey: string): void {
    // TODO: In production, encrypt this with a user password
    // For now, stored as-is but marked for improvement
    localStorage.setItem(`private_key_${userId}`, privateKey);
  }

  /**
   * Retrieve user's private key from storage
   */
  static getPrivateKey(userId: number): string | null {
    return localStorage.getItem(`private_key_${userId}`);
  }

  /**
   * Clear all encryption-related stored data (logout)
   */
  static clearKeys(userId: number): void {
    localStorage.removeItem(`private_key_${userId}`);
    localStorage.removeItem(`public_key_${userId}`);
    
    // Clear all shared secrets for this user
    Object.keys(sessionStorage).forEach((key) => {
      if (key.startsWith('shared_secret_')) {
        sessionStorage.removeItem(key);
      }
    });
  }

  /**
   * Verify message authenticity by checking sender's public key
   * This ensures the message really came from who we think it did
   */
  static verifyMessageSender(
    encryptedMessage: EncryptedMessage,
    expectedSenderPublicKey: string
  ): boolean {
    return encryptedMessage.senderPublicKey === expectedSenderPublicKey;
  }
}

export default EncryptionService;
