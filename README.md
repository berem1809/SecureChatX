# SecureChatX - End-to-End Encrypted Chat Application

A full-stack secure messaging application with **End-to-End Encryption (E2EE)**, built using **Spring Boot** and **React with TypeScript**. The server never sees your messages in plain text!

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
| Bucket4j | 8.7.0 | Rate Limiting |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.2.0 | UI Library |
| TypeScript | 5.2.2 | Type-Safe JavaScript |
| Vite | 5.2.0 | Build Tool |
| Redux Toolkit | 2.11.2 | State Management |
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
