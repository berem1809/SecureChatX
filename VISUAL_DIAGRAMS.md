# Visual Diagrams: E2EE Chat System

## 1. Complete Message Flow (What Happens When Users Chat)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         END-TO-END ENCRYPTION FLOW                              │
└─────────────────────────────────────────────────────────────────────────────────┘

STEP 1: LOGIN & INITIALIZATION
═════════════════════════════════════════════════════════════════════════════════

Alice's Browser                           Server                     MySQL Database
────────────────────────────────────────────────────────────────────────────────

1. Login form
   ├─ email: alice@test.com
   └─ password: Pass123!
          │
          ├──────── POST /api/auth/login ───────→ Validate credentials
          │                                       │
          │                                       └─→ Check user.status = 'ACTIVE'
          │                                           │
          │                        ← Return JWT token ←─
          │
2. App.tsx detects login ✅
   ├─ Calls initializeUserEncryption()
   │
3. Generate X25519 Keypair
   ├─ Public Key:  Base64(alice_pub)
   └─ Private Key: Base64(alice_priv)
          │
4. Store Private Key
   ├─ localStorage['private_key_alice_id'] = Base64(alice_priv)
   │  ⚠️  NEVER sent to server!
   │
5. Upload Public Key
   ├──── POST /api/crypto/keys/public ───→ Store in UserEncryptionKey table
   │     {publicKey: Base64(alice_pub)}    │
   │                                       └─→ user_encryption_keys[alice_id]
   │                 ← 201 Created ←──────
   │
6. Browser Console
   └─→ "✅ Encryption initialized successfully"


STEP 2: SENDING MESSAGE (Encryption Happens Here)
═════════════════════════════════════════════════════════════════════════════════

Alice's Browser                           Server                     MySQL Database
────────────────────────────────────────────────────────────────────────────────

1. Alice types "Hello Bob!" in chat

2. Click Send → sendMessage thunk starts

3. Fetch Bob's public key
   ├──── GET /api/crypto/keys/public/bob_id ───→ Lookup bob_pub
   │                                             │
   │                      ← {publicKey: bob_pub} ←─
   │
4. Get Alice's private key from localStorage
   ├─ privateKey = localStorage['private_key_alice_id']  ✅ Available!
   │
5. ECDH Key Agreement (X25519)
   ├─ sharedSecret = ECDH(alice_priv, bob_pub)
   │  [Produces same secret on Bob's side with: ECDH(bob_priv, alice_pub)]
   │
6. Encryption (XSalsa20-Poly1305)
   ├─ nonce = random 24 bytes
   ├─ plaintext = "Hello Bob!"
   ├─ ciphertext = XSalsa20(plaintext, sharedSecret, nonce)
   │             + Poly1305 MAC (authentication)
   │
7. Send Encrypted Message
   ├──── POST /api/conversations/1/messages ───→ Store in messages table:
   │     {                                        │
   │       encryptedContent: Base64(ciphertext),  ├─→ messages[
   │       encryptionNonce: Base64(nonce),        │      id: 1,
   │       senderPublicKey: Base64(alice_pub),    │      encrypted_content: '...',
   │       isEncrypted: true                      │      encryption_nonce: '...',
   │     }                                        │      sender_public_key: '...',
   │                                              │      sender_id: alice_id
   │              ← 201 Created ←──────          │    ]
   │
8. Message Stored Encrypted! ✅
   └─→ Server cannot read it (only has ciphertext)


STEP 3: RECEIVING MESSAGE (Decryption Happens Here)
═════════════════════════════════════════════════════════════════════════════════

Bob's Browser                             Server                     MySQL Database
────────────────────────────────────────────────────────────────────────────────

1. Bob opens chat conversation

2. Fetch Messages
   ├──── GET /api/conversations/1/messages ───→ SELECT from messages table
   │                                             │
   │      ← [{                              ←───┤
   │         id: 1,
   │         encryptedContent: Base64(...),
   │         encryptionNonce: Base64(...),
   │         senderPublicKey: Base64(alice_pub),
   │         isEncrypted: true
   │       }]
   │
3. Get Bob's private key from localStorage
   ├─ privateKey = localStorage['private_key_bob_id']  ✅ Available!
   │
4. Extract Sender's Public Key
   ├─ senderPublicKey = message.senderPublicKey = Base64(alice_pub)
   │
5. ECDH Key Agreement (X25519)
   ├─ sharedSecret = ECDH(bob_priv, alice_pub)
   │  [Same result as Alice's: ECDH(alice_priv, bob_pub)]
   │
6. Decryption (XSalsa20-Poly1305)
   ├─ ciphertext = Base64_decode(message.encryptedContent)
   ├─ nonce = Base64_decode(message.encryptionNonce)
   ├─ plaintext = XSalsa20_decrypt(ciphertext, sharedSecret, nonce)
   ├─ Verify Poly1305 MAC (ensure not tampered)
   │
7. Display Message
   ├─ plaintext = "Hello Bob!"
   └─→ UI renders message ✅


STEP 4: SECURITY PROPERTIES
═════════════════════════════════════════════════════════════════════════════════

Confidentiality:   ✅ Only Alice & Bob know sharedSecret (others can't decrypt)
Authenticity:      ✅ Poly1305 MAC detects if message tampered
Non-Repudiation:   ⚠️  Not yet implemented (could add digital signatures)
Forward Secrecy:   ✅ Old messages safe even if keys stolen (random nonce)
Server-Blind:      ✅ Server stores only ciphertext (can't read messages)
Private Keys:      ✅ NEVER leave client device (stored only in localStorage)
```

---

## 2. What Changed to Fix the Errors

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           BEFORE vs AFTER FIX                                │
└──────────────────────────────────────────────────────────────────────────────┘

BEFORE (❌ BROKEN):
═════════════════════════════════════════════════════════════════════════════════

User Login
  │
  ├─→ JWT token received                           ✅
  │
  ├─→ No encryption initialization                 ❌ (No trigger!)
  │   └─→ Private key not generated                ❌
  │   └─→ Public key not uploaded                  ❌
  │
User sends message
  │
  ├─→ sendMessage thunk expects caller to pass:    ❌ (Caller doesn't!)
  │   ├─ recipientPublicKey
  │   ├─ myPrivateKey
  │   ├─ myPublicKey
  │   └─ myUserId
  │
  ├─→ All undefined                                ❌
  │
  └─→ Message send fails                           ❌ Error


AFTER (✅ FIXED):
═════════════════════════════════════════════════════════════════════════════════

User Login
  │
  ├─→ JWT token received                           ✅
  │
  ├─→ App.tsx AUTOMATICALLY initializes            ✅ (New hook!)
  │   ├─→ useEffect(() => { if (user) init() }, [user])
  │   ├─→ Check if has public key on server
  │   ├─→ If not, generate keypair
  │   ├─→ Store private key in localStorage        ✅
  │   ├─→ Upload public key to server              ✅
  │   └─→ Console: "✅ Encryption initialized"     ✅
  │
User sends message
  │
  ├─→ sendMessage thunk FETCHES keys at send time  ✅ (New logic!)
  │   ├─→ Get recipient ID from selectedConversation
  │   ├─→ Fetch recipient's public key from server ✅ (Now exists!)
  │   ├─→ Get own private key from localStorage    ✅ (Now exists!)
  │   ├─→ Fetch own public key from server         ✅ (Now exists!)
  │
  ├─→ All keys available                           ✅
  │
  ├─→ Derive shared secret                         ✅
  ├─→ Encrypt message                              ✅
  ├─→ Send to server                               ✅
  │
  └─→ Message send succeeds!                       ✅
```

---

## 3. Key Storage Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            WHERE KEYS ARE STORED                             │
└──────────────────────────────────────────────────────────────────────────────┘

Alice's Device:
═════════════════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────┐
│   Alice's Browser Memory            │
├─────────────────────────────────────┤
│ JWT Token (from login)              │  ← Authorization header for requests
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   Browser localStorage              │
├─────────────────────────────────────┤
│ private_key_alice_id:               │
│   "Base64(alice_private_key)"       │  ← 🔐 CRITICAL! Never leave device
│                                     │
│ accessToken:                        │
│   "eyJhbGci..."                     │  ← JWT token for auth
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   Browser sessionStorage            │
├─────────────────────────────────────┤
│ shared_secret_1_bob_id:             │
│   {                                 │  ← Cached for current session only
│     sharedSecret: [32-byte array],  │     Cleared on logout
│     theirPublicKey: "Base64(...)"   │
│   }                                 │
└─────────────────────────────────────┘


Server Database (MySQL):
═════════════════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────┐
│   users table                       │
├─────────────────────────────────────┤
│ id    | email          | status     │
│ 1     | alice@test.com | ACTIVE     │
│ 2     | bob@test.com   | ACTIVE     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   user_encryption_keys table        │
├─────────────────────────────────────┤
│ user_id | public_key                │
│ 1       | "Base64(alice_pub)"       │  ← Safe to store! Used for key exchange
│ 2       | "Base64(bob_pub)"         │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   messages table                    │
├─────────────────────────────────────┤
│ id | encrypted_content              │
│ 1  | "Base64(XSalsa20 ciphertext)"  │  ← Unreadable! Encrypted on client
│                                     │
│    | encryption_nonce               │
│    | "Base64(24-byte nonce)"        │
│                                     │
│    | sender_public_key               │
│    | "Base64(alice_pub)"            │  ← Needed for decryption
└─────────────────────────────────────┘


Key Derivation Process:
═════════════════════════════════════════════════════════════════════════════════

Sender (Alice):
  private_key_alice (from localStorage) + public_key_bob (from server)
                          │
                          ├─→ ECDH(alice_priv, bob_pub)
                          │
                          └─→ sharedSecret_alice_bob

Recipient (Bob):
  private_key_bob (from localStorage) + public_key_alice (from message)
                          │
                          ├─→ ECDH(bob_priv, alice_pub)
                          │
                          └─→ sharedSecret_bob_alice (SAME as above!)

Both derive same secret without it ever being transmitted! 🔒
```

---

## 4. Error Resolution Tree

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                   DEBUGGING: Which Error Are You Seeing?                     │
└──────────────────────────────────────────────────────────────────────────────┘

START: Try to send message
  │
  └─→ ERROR: "POST /api/auth/login 400"
      │
      └─→ Root Cause: User account not verified
          │
          Solution: UPDATE users SET status='ACTIVE' WHERE email='...';
          │
          Test: Login should work now


START: Try to send message
  │
  └─→ ERROR: "GET /api/crypto/keys/public/X 404"
      │
      └─→ Root Cause: User X hasn't uploaded encryption key
          │
          ├─→ Sub-cause: Encryption init never ran
          │   │
          │   └─→ Why: No trigger for initialization
          │       │
          │       Solution: ✅ FIXED - App.tsx now auto-initializes
          │
          ├─→ How to fix NOW:
          │   │
          │   ├─→ Refresh browser (F5)
          │   │
          │   └─→ Wait for console message:
          │       "✅ Encryption initialized successfully"
          │
          └─→ If still 404: Check localStorage
              │
              └─→ In DevTools Console:
                  localStorage.getItem('private_key_X')
                  │
                  Should return long Base64 string
                  │
                  If empty → Initialization failed


START: Try to send message
  │
  └─→ ERROR: "Failed to decrypt message"
      │
      └─→ Root Cause: Recipient's key not available or wrong
          │
          ├─→ Check 1: Is recipient initialized?
          │   └─→ Refresh their browser window
          │
          ├─→ Check 2: Both public keys on server?
          │   └─→ Verify: /api/crypto/keys/public/{id} returns 200
          │
          └─→ Check 3: Private keys in localStorage?
              └─→ Both users need their private keys stored


SUCCESS INDICATORS:
═════════════════════════════════════════════════════════════════════════════════

✅ After login, browser console shows:
   "✅ Encryption initialized successfully"
   "🔒 Private key stored locally"
   "📤 Public key uploaded to server"

✅ In Network tab (DevTools), message send shows:
   POST /api/conversations/X/messages 201 Created
   Payload includes: encryptedContent, encryptionNonce, senderPublicKey

✅ In MySQL, you can verify:
   SELECT * FROM user_encryption_keys;
   (Both users should have entries)

✅ Message appears in recipient's chat
   (Means decryption worked on their end)
```

---

## 5. ECDH Key Exchange Visualization

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                  HOW ECDH (X25519) KEY EXCHANGE WORKS                        │
└──────────────────────────────────────────────────────────────────────────────┘

Initial Setup (Happens at login):
═════════════════════════════════════════════════════════════════════════════════

Alice's Device                              Server                    Bob's Device
───────────────────────────────────────────────────────────────────────────────

Generate keypair                          Store public keys          Generate keypair
alice_priv (secret!)                      in database                bob_priv (secret!)
alice_pub (public)  ────→  Store in DB                    ←──── Store in DB
                           alice_pub                             bob_pub

Store locally:
private_key_alice                         
(never sent!)


Messaging (Both parties independently derive same secret):
═════════════════════════════════════════════════════════════════════════════════

Alice's Process:                          Bob's Process:
────────────────────                      ──────────────
alice_priv (local)                        bob_priv (local)
        +                                        +
bob_pub (fetched)  ──→ ECDH(X25519) ←── alice_pub (fetched)
        │                  │                     │
        ├─→ sharedSecret_AB          sharedSecret_AB ←─┤
            (32 bytes)                 (32 bytes)
            Same secret! No transmission needed!


Why This Is Secure:
═════════════════════════════════════════════════════════════════════════════════

1. Only Alice knows alice_priv (on her device)
2. Only Bob knows bob_priv (on his device)
3. ECDH math is such that:
   ECDH(alice_priv, bob_pub) == ECDH(bob_priv, alice_pub)
4. But server watching traffic sees only:
   - alice_pub (public - safe to share)
   - bob_pub (public - safe to share)
5. Server CANNOT derive sharedSecret without private keys
6. Attacker needs either alice_priv OR bob_priv to decrypt

Result: Only Alice & Bob can read each other's messages! 🔒


Message Encryption Flow Using Shared Secret:
═════════════════════════════════════════════════════════════════════════════════

Alice:                                    Bob:
──────                                    ────
plaintext = "Hello!"                      ciphertext + nonce
        │                                        ↑
        ├─→ XSalsa20-Poly1305            ←──────┤
            ├─ key = sharedSecret_AB
            ├─ nonce = random 24 bytes
            ├─ ciphertext = encrypt(plaintext, key, nonce)
            ├─ mac = authenticate(ciphertext, key)
            │
            └─→ send(ciphertext, nonce, mac, alice_pub)
                        │
                        ├─→ Server stores encrypted ✅
                        │   (can't read it!)
                        │
                        └─→ Bob receives
                            ├─ Derives sharedSecret_AB (same as Alice!)
                            ├─ Verifies mac (not tampered)
                            ├─ Decrypts ciphertext
                            └─ Reads plaintext ✅
```

---

## 6. Before/After Architecture Comparison

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                      BEFORE THE FIX (Broken)                                 ║
╚══════════════════════════════════════════════════════════════════════════════╝

Frontend: ChatPage.tsx                   Redux: chatSlice.ts                Backend
──────────────────────                   ──────────────────                ──────
User clicks "Send"
    │
    └─→ dispatch(sendMessage({
        conversationId: 123,
        content: "Hello"
    }))
            │
            └─→ sendMessage thunk expects:
                {
                  conversationId,
                  content,
                  recipientPublicKey,  ← ❌ NOT PROVIDED!
                  myPrivateKey,        ← ❌ NOT PROVIDED!
                  myPublicKey,         ← ❌ NOT PROVIDED!
                  myUserId             ← ❌ NOT PROVIDED!
                }
            │
            ├─→ All undefined ❌
            │
            └─→ Error! ❌


╔══════════════════════════════════════════════════════════════════════════════╗
║                      AFTER THE FIX (Working)                                 ║
╚══════════════════════════════════════════════════════════════════════════════╝

App.tsx                                  Frontend: ChatPage.tsx
──────                                   ──────────────────────
User logs in ✅
    │
    └─→ useEffect detects user loaded
        │
        └─→ initializeUserEncryption()
            ├─ Check if public key on server
            ├─ If not: generate keypair
            ├─ Store private key: localStorage
            └─ Upload public key: server ✅


Redux: chatSlice.ts                      Backend
──────────────────                       ──────
User clicks "Send"
    │
    └─→ dispatch(sendMessage({
        conversationId: 123,
        content: "Hello"
    }))
            │
            └─→ sendMessage thunk:
                {
                  conversationId,
                  content
                }
            │
            ├─→ Get user from getState()
            ├─→ Get conversation from getState()
            │
            ├─→ Fetch recipientPublicKey    ← ✅ FROM SERVER!
            │   api.get(/api/crypto/keys/public/{recipientId})
            │
            ├─→ Get myPrivateKey            ← ✅ FROM LOCALSTORAGE!
            │   localStorage.getItem('private_key_' + user.id)
            │
            ├─→ Fetch myPublicKey           ← ✅ FROM SERVER!
            │   api.get(/api/crypto/keys/public/{user.id})
            │
            └─→ Now encrypt and send ✅


Result: Message encrypted and sent successfully! ✅
```

---

These diagrams show the complete flow of what was broken and how it's fixed now!
