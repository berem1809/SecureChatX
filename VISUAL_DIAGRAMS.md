# Visual Diagrams: E2EE Chat System Architecture

## 1. Complete System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CHAT SYSTEM OVERVIEW                                │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────┐
│      FRONTEND (React/TS)         │
├─────────────────────────────────┤
│ ✓ App.tsx                        │
│   └─ Auto-init encryption       │
│                                 │
│ ✓ ChatPage.tsx                 │
│   ├─ Display conversations     │
│   ├─ List messages             │
│   └─ Send messages             │
│                                 │
│ ✓ Redux Store                  │
│   ├─ User state                │
│   ├─ Conversations             │
│   └─ Messages (encrypted)      │
└────────────┬────────────────────┘
             │
             │ HTTP/REST API
             │
┌────────────▼────────────────────┐
│    BACKEND (Spring Boot)         │
├─────────────────────────────────┤
│ ✓ AuthController                │
│   └─ POST /api/auth/login       │
│                                 │
│ ✓ CryptoController              │
│   ├─ POST /api/crypto/keys      │
│   └─ GET /api/crypto/keys/{id}  │
│                                 │
│ ✓ ConversationController        │
│   ├─ GET /api/conversations     │
│   └─ POST /api/conversations/{id}/messages
│                                 │
│ ✓ Services (Business Logic)     │
│   ├─ UserService               │
│   ├─ ConversationService       │
│   └─ EncryptionService         │
└────────────┬────────────────────┘
             │
             │ JPA/Hibernate
             │
┌────────────▼────────────────────┐
│   DATABASE (MySQL)               │
├─────────────────────────────────┤
│ ✓ users                         │
│ ✓ conversations                 │
│ ✓ messages                      │
│ ✓ user_encryption_keys         │
└─────────────────────────────────┘


SECURITY LAYERS:
═════════════════════════════════════════════════════════════════════════════════

Layer 1: Authentication          JWT Token ────→ Verify on each request
Layer 2: Transport Security      HTTPS ────→ Encrypted in transit
Layer 3: End-to-End Encryption   ECDH + XSalsa20-Poly1305 ────→ Server-blind
```

---

## 2. Message Flow: Send to Receive

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    MESSAGE LIFECYCLE                                        │
└─────────────────────────────────────────────────────────────────────────────┘

ALICE SENDS MESSAGE TO BOB:
═════════════════════════════════════════════════════════════════════════════════

Alice's Browser                  Server              Bob's Browser
───────────────────              ──────              ─────────────

1. User types "Hi Bob!"
   │
2. Click SEND
   │
3. Get Bob's public key
   ├──── GET /api/crypto/keys/{bobId} ────→ Database lookup
   │                                        │
   │                     ← {publicKey} ←────┤
   │
4. Get own keys from localStorage
   ├─ privateKey = localStorage['private_key_aliceId']
   │
5. Derive Shared Secret
   ├─ secret = ECDH(alice_priv, bob_pub)
   │
6. Encrypt Message
   ├─ nonce = random(24 bytes)
   ├─ ciphertext = XSalsa20(message, secret, nonce)
   ├─ mac = Poly1305(ciphertext, secret)
   │
7. Send Encrypted
   ├──── POST /api/conversations/{id}/messages ────→ Store:
   │     {                                            │ encrypted_content
   │       encryptedContent: ciphertext               │ encryption_nonce
   │       encryptionNonce: nonce                    │ sender_public_key
   │       senderPublicKey: alice_pub                │ sender_id
   │     }                                            │
   │                      ← 201 CREATED ←────────────┤
   │
   Server stored! (Can't read it) ✅
                                                    8. Bob opens chat
                                                       │
                                                    9. Fetch Messages
                                                       ├──── GET /messages ───→
                                                       │
                                                       ← [message] ←──
                                                       │
                                                    10. Get own private key
                                                       ├─ privateKey = localStorage
                                                       │
                                                    11. Derive Shared Secret
                                                       ├─ secret = ECDH(bob_priv, alice_pub)
                                                       │  (Same secret as Alice!)
                                                       │
                                                    12. Decrypt Message
                                                       ├─ plaintext = XSalsa20_decrypt(...)
                                                       ├─ Verify MAC (authentic)
                                                       │
                                                    13. Display "Hi Bob!"
                                                        UI shows message ✅
```

---

## 3. Key Storage & Generation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    WHERE ENCRYPTION KEYS LIVE                               │
└─────────────────────────────────────────────────────────────────────────────┘

ALICE'S DEVICE:
═════════════════════════════════════════════════════════════════════════════════

Browser Memory (RAM)
├─ JWT Token (from login)

Browser localStorage (Persistent)
├─ private_key_aliceId = "Base64(32-byte private key)" 🔐 CRITICAL
├─ accessToken = "eyJhbGc..."
└─ conversationId = "123"

Browser sessionStorage (Current Session Only)
├─ shared_secret_1_bob = {secret, timestamp}
└─ (Cleared on logout/refresh)


SERVER DATABASE (MySQL):
═════════════════════════════════════════════════════════════════════════════════

users table
├─ id | email          | status
├─ 1  | alice@test.com | ACTIVE
└─ 2  | bob@test.com   | ACTIVE

user_encryption_keys table
├─ id | user_id | public_key                    (Safe! Public!)
├─ 1  | 1       | "Base64(alice_public_key)"
└─ 2  | 2       | "Base64(bob_public_key)"

conversations table
├─ id | user_id_1 | user_id_2 | created_at
└─ 1  | 1         | 2         | 2024-01-01

messages table
├─ id | conversation_id | sender_id | encrypted_content | encryption_nonce | sender_public_key
├─ 1  | 1              | 1         | "aB3dE5f..."      | "xYz..."         | "Base64(alice_pub)"
└─ (All encrypted! Server can't read)


KEY GENERATION TIMELINE:
═════════════════════════════════════════════════════════════════════════════════

LOGIN
  ├─→ Check if public key exists on server
  │
  ├─→ NO: Generate keypair (X25519)
  │   ├─ Private Key (32 bytes) → localStorage [SECRET]
  │   ├─ Public Key (32 bytes)  → Upload to server [PUBLIC]
  │
  └─→ YES: Keys already exist (skip generation)
```

---

## 4. ECDH Key Exchange (Simplified)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              HOW ALICE & BOB SHARE A SECRET (Without Server)                │
└─────────────────────────────────────────────────────────────────────────────┘

SETUP (at login):
═════════════════════════════════════════════════════════════════════════════════

Alice generates:                  Bob generates:
├─ alice_private_key              ├─ bob_private_key
│  (32 bytes, SECRET)             │  (32 bytes, SECRET)
│  Stored: localStorage            │  Stored: localStorage
│                                  │
├─ alice_public_key               ├─ bob_public_key
│  (32 bytes, PUBLIC)             │  (32 bytes, PUBLIC)
│  Stored: Server DB               │  Stored: Server DB


MESSAGING (Both derive same secret independently):
═════════════════════════════════════════════════════════════════════════════════

Alice sends to Bob:

  Step 1: Get Bob's public key from server
          └─ bob_public_key ✅
          
  Step 2: Use Alice's private key from localStorage
          └─ alice_private_key ✅
          
  Step 3: ECDH(alice_private_key, bob_public_key)
          └─ shared_secret = 32-byte value
          
  Step 4: Encrypt message with shared_secret ✅


Bob receives from Alice:

  Step 1: Get Alice's public key from message
          └─ alice_public_key ✅
          
  Step 2: Use Bob's private key from localStorage
          └─ bob_private_key ✅
          
  Step 3: ECDH(bob_private_key, alice_public_key)
          └─ shared_secret = SAME 32-byte value! 🔒
          
  Step 4: Decrypt message with shared_secret ✅


WHY IT'S SECURE:
═════════════════════════════════════════════════════════════════════════════════

✓ Server knows: alice_public_key, bob_public_key
✗ Server knows: alice_private_key (NO - not transmitted)
✗ Server knows: bob_private_key (NO - not transmitted)
✗ Server can derive: shared_secret (NO - needs private key)

Result: Only Alice & Bob can read each other's messages! 🔐
```

---

## 5. Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE DATA FLOW                                       │
└─────────────────────────────────────────────────────────────────────────────┘

USER LOGIN FLOW:
═════════════════════════════════════════════════════════════════════════════════

User Input (email, password)
        │
        ├─→ AuthController: POST /api/auth/login
        │   └─ Check credentials in DB
        │   └─ Return JWT token ✅
        │
        ├─→ Frontend stores JWT
        │   └─ localStorage['accessToken']
        │
        ├─→ App.tsx detects user logged in
        │   └─ useEffect triggers
        │
        ├─→ initializeUserEncryption()
        │   │
        │   ├─ Check: Do I have public key on server?
        │   │   └─ GET /api/crypto/keys/public/{userId}
        │   │
        │   ├─ NO → Generate keypair
        │   │   ├─ X25519.generateKeyPair()
        │   │   ├─ Store private: localStorage
        │   │   └─ Upload public: POST /api/crypto/keys
        │   │
        │   └─ YES → Already initialized ✅


SENDING MESSAGE FLOW:
═════════════════════════════════════════════════════════════════════════════════

User clicks SEND
        │
        ├─→ ChatPage dispatches: sendMessage action
        │   {conversationId, content}
        │
        ├─→ sendMessage thunk fetches keys:
        │
        │   1. Get sender ID and recipient ID
        │      └─ from Redux store
        │
        │   2. GET /api/crypto/keys/public/{recipientId}
        │      └─ Receive recipient_public_key
        │
        │   3. localStorage['private_key_' + userId]
        │      └─ Get sender's private key
        │
        │   4. ECDH(sender_private, recipient_public)
        │      └─ Derive shared_secret
        │
        │   5. Encrypt(content, shared_secret)
        │      └─ Generate encrypted_content + nonce
        │
        │   6. POST /api/conversations/{id}/messages
        │      Payload:
        │      {
        │        encryptedContent: "...",
        │        encryptionNonce: "...",
        │        senderPublicKey: "..."
        │      }
        │
        │   7. Server stores encrypted message
        │      └─ Response: 201 CREATED ✅
        │
        └─→ Thunk dispatches: ADD_MESSAGE to Redux


RECEIVING MESSAGE FLOW:
═════════════════════════════════════════════════════════════════════════════════

Recipient opens chat
        │
        ├─→ ChatPage: useEffect fetches messages
        │   └─ GET /api/conversations/{id}/messages
        │
        ├─→ Redux receives encrypted message array
        │   ├─ encryptedContent
        │   ├─ encryptionNonce
        │   └─ senderPublicKey
        │
        ├─→ UI render loop decrypts each message:
        │
        │   1. Get senderPublicKey from message
        │      └─ alice_public_key
        │
        │   2. localStorage['private_key_' + myId]
        │      └─ bob_private_key
        │
        │   3. ECDH(my_private, sender_public)
        │      └─ shared_secret (same as sender!)
        │
        │   4. Decrypt(encryptedContent, shared_secret)
        │      └─ plaintext = "Hi Bob!"
        │
        │   5. Display in UI ✅
        │      └─ User reads message
```

---

## 6. Error Handling

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    COMMON ERRORS & SOLUTIONS                                │
└─────────────────────────────────────────────────────────────────────────────┘

ERROR 1: "401 Unauthorized"
═════════════════════════════════════════════════════════════════════════════════
Cause:   JWT token invalid or user account inactive
Fix:     ├─ Check: user.status = 'ACTIVE' in database
         └─ Re-login to get fresh JWT token


ERROR 2: "404 - Public key not found"
═════════════════════════════════════════════════════════════════════════════════
Cause:   Recipient hasn't uploaded encryption key
Fix:     ├─ Recipient: Refresh browser (F5)
         ├─ Wait for console: "✅ Encryption initialized"
         └─ Check localStorage: should have 'private_key_...' key


ERROR 3: "Failed to encrypt/decrypt message"
═════════════════════════════════════════════════════════════════════════════════
Cause:   Missing or corrupted keys in localStorage
Fix:     ├─ Open DevTools Console
         ├─ Check: localStorage.getItem('private_key_X')
         ├─ If empty: Clear localStorage and re-login
         └─ Try again


SUCCESS CHECKS:
═════════════════════════════════════════════════════════════════════════════════
✅ After login:
   Console: "✅ Encryption initialized successfully"

✅ In Network tab (DevTools):
   POST /api/conversations/X/messages → 201 Created

✅ In Database:
   SELECT * FROM user_encryption_keys WHERE user_id = 1;
   (Should have entry with public key)

✅ Messages appear in recipient's chat
   (Means decryption succeeded)
```

---

## 7. Technology Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    TOOLS & LIBRARIES USED                                   │
└─────────────────────────────────────────────────────────────────────────────┘

FRONTEND:
═════════════════════════════════════════════════════════════════════════════════
✓ React 18              - UI framework
✓ TypeScript            - Type safety
✓ Redux Toolkit         - State management
✓ Axios                 - HTTP client
✓ TweetNaCl.js          - Encryption (ECDH, XSalsa20, Poly1305)
✓ Base64 (js-base64)    - Encoding/Decoding


BACKEND:
═════════════════════════════════════════════════════════════════════════════════
✓ Spring Boot 3         - Framework
✓ Spring Security       - JWT authentication
✓ JPA/Hibernate         - ORM
✓ MySQL Driver          - Database client
✓ Lombok                - Reduce boilerplate
✓ Maven                 - Build tool


DATABASE:
═════════════════════════════════════════════════════════════════════════════════
✓ MySQL 8.0             - Relational database
✓ InnoDB                - Storage engine (ACID transactions)


ENCRYPTION ALGORITHMS:
═════════════════════════════════════════════════════════════════════════════════
✓ X25519                - Key exchange (ECDH)
✓ XSalsa20              - Symmetric encryption
✓ Poly1305              - Message authentication code (MAC)
```

---

These simplified diagrams provide accurate, easy-to-understand visualizations of the entire system!
