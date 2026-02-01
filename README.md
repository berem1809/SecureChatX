# SecureChatX - End-to-End Encrypted Chat Application

A full-stack secure messaging application with **End-to-End Encryption (E2EE)**, built using **Spring Boot** and **React with TypeScript**. The server never sees your messages in plain text!

---

## 📋 Table of Contents

- [About the Project](#about-the-project)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Architecture Overview](#architecture-overview)

---

## 🔐 About the Project

**SecureChatX** is a privacy-focused chat application that implements true End-to-End Encryption. Messages are encrypted on the client side before being sent to the server, ensuring that only the intended recipients can read them. The server stores encrypted data and has no access to the plaintext content.

This project demonstrates modern security practices including:
- **ECDH Key Exchange** for establishing shared secrets
- **XSalsa20-Poly1305** encryption for messages
- **JWT-based authentication** with access and refresh tokens
- **Redis-backed token management** with automatic expiration

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| 🔒 **End-to-End Encryption** | Messages encrypted client-side using ECDH + XSalsa20-Poly1305. Server-blind architecture. |
| 👥 **User Management** | Registration, email verification, secure login with JWT tokens |
| 💬 **Real-time Messaging** | WebSocket support for instant message delivery |
| 📨 **Chat Requests** | Friend request system before starting conversations |
| 👨‍👩‍👧‍👦 **Group Chats** | Create and manage group conversations with encrypted group keys |
| 🔔 **Notifications** | Real-time notification system for new messages and requests |
| 🛡️ **Rate Limiting** | Built-in protection against abuse with Bucket4j |
| 📱 **Responsive UI** | Material UI components for a clean, modern interface |

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming Language |
| Spring Boot | 3.1.4 | Application Framework |
| Spring Security | 6.x | Authentication & Authorization |
| Spring Data JPA | 3.x | Database ORM |
| MySQL | 8.x | Primary Database |
| Redis | 7.x | Token Storage (TTL-based) |
| JWT (jjwt) | 0.11.5 | Token Management |
| Lombok | 1.18.30 | Code Generation |
| Bucket4j | 8.7.0 | Rate Limiting |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.2.0 | UI Library |
| TypeScript | 5.2.2 | Type-Safe JavaScript |
| Vite | 5.2.0 | Build Tool |
| Redux Toolkit | 2.11.2 | State Management |
| React Router | 7.11.0 | Client-Side Routing |
| Material UI | 7.3.6 | UI Components |
| Axios | 1.4.0 | HTTP Client |
| TweetNaCl | 1.0.3 | Cryptographic Library |
| Socket.io Client | 4.7.2 | WebSocket Client |

---

## 📌 Requirements

### Software Prerequisites

| Software | Minimum Version | Download Link |
|----------|-----------------|---------------|
| **JDK** | 21+ | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) / [OpenJDK](https://adoptium.net/) |
| **Node.js** | 18+ | [Node.js](https://nodejs.org/) |
| **npm** | 9+ | Comes with Node.js |
| **MySQL** | 8.0+ | [MySQL](https://dev.mysql.com/downloads/) |
| **Redis** | 7.0+ | [Redis](https://redis.io/download/) |
| **Maven** | 3.8+ | [Maven](https://maven.apache.org/download.cgi) |

### Verify Installation

```bash
# Check Java version
java -version

# Check Node.js version
node -v

# Check npm version
npm -v

# Check Maven version
mvn -v

# Check MySQL (after starting service)
mysql --version

# Check Redis (after starting service)
redis-cli ping
```

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/berem1809/SecureChatX.git
cd SecureChatX
```

### 2. Database Setup

```bash
# Start MySQL service and create database
mysql -u root -p
```

```sql
CREATE DATABASE chat_springboot_app;
EXIT;
```

### 3. Redis Setup

```bash
# Start Redis server
redis-server

# Verify Redis is running
redis-cli ping
# Should return: PONG
```

### 4. Backend Configuration

Navigate to `backend/src/main/resources/application.properties` and update:

```properties
# MySQL Configuration
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

# JWT Secret (generate a secure random string)
app.jwt.secret=YOUR_JWT_SECRET_KEY

# Gmail SMTP (for email verification)
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### 5. Start Backend Server

```bash
cd backend
mvn spring-boot:run
```

Backend runs at: `http://localhost:8080`

### 6. Start Frontend Development Server

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at: `http://localhost:5173`

---

## 📁 Project Structure

```
chat-app/
├── backend/                    # Spring Boot Backend
│   ├── src/main/java/com/chatapp/
│   │   ├── config/            # Security, Redis, Rate Limit configs
│   │   ├── controller/        # REST API endpoints
│   │   ├── dto/               # Data Transfer Objects
│   │   ├── exception/         # Custom exceptions
│   │   ├── filter/            # JWT filters
│   │   ├── model/             # JPA Entities
│   │   ├── repository/        # Data repositories
│   │   ├── security/          # JWT utilities
│   │   ├── service/           # Business logic
│   │   └── util/              # Helper utilities
│   └── src/main/resources/
│       └── application.properties
│
├── frontend/                   # React + TypeScript Frontend
│   ├── src/
│   │   ├── components/        # Reusable UI components
│   │   ├── context/           # React contexts
│   │   ├── pages/             # Page components
│   │   ├── services/          # API & encryption services
│   │   ├── store/             # Redux store & slices
│   │   └── types/             # TypeScript interfaces
│   └── package.json
│
└── README.md                   # This file
```

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SYSTEM ARCHITECTURE                                  │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────┐       ┌─────────────────────────────┐
│   Frontend (React + TS)      │       │   Backend (Spring Boot)      │
├─────────────────────────────┤       ├─────────────────────────────┤
│ • E2E Encryption (TweetNaCl)│       │ • REST API Controllers      │
│ • Redux State Management    │◄─────►│ • JWT Authentication        │
│ • Material UI Components    │ HTTPS │ • WebSocket Support         │
│ • React Router Navigation   │       │ • Business Services         │
└─────────────────────────────┘       └──────────────┬──────────────┘
                                                      │
                                      ┌───────────────┼───────────────┐
                                      │               │               │
                               ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
                               │   MySQL      │ │   Redis      │ │   SMTP      │
                               │   (Users,    │ │   (Tokens,   │ │   (Email    │
                               │   Messages)  │ │   Sessions)  │ │   Verify)   │
                               └─────────────┘ └─────────────┘ └─────────────┘

SECURITY LAYERS:
═══════════════════════════════════════════════════════════════════════════════
Layer 1: Authentication       → JWT Token verification on each request
Layer 2: Transport Security   → HTTPS encrypted in transit
Layer 3: End-to-End Encryption → ECDH + XSalsa20-Poly1305 (Server-blind)
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
