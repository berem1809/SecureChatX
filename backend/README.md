# ChatApp Backend

Spring Boot authentication service with JWT, MySQL, and Redis.

## Tech Stack

- **Java 21** + **Spring Boot 3.1.4**
- **MySQL** - User storage
- **Redis** - Token storage (TTL-based)
- **JWT** - Access (15min) & Refresh (30d) tokens
- **BCrypt** - Password hashing
- **Gmail SMTP** - Email verification

## Quick Start

`ash
# Prerequisites: MySQL, Redis running

# Create database
mysql -u root -p -e "CREATE DATABASE chat_springboot_app;"

# Configure application.properties (MySQL password, Gmail app password, JWT secret)

# Run
mvn spring-boot:run
`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register user |
| GET | `/api/auth/verify?token=xxx` | Verify email |
| POST | `/api/auth/login` | Login (returns JWT) |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Logout |

## Test with cURL

`ash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","displayName":"Test"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!"}' -c cookies.txt
`

## Architecture

`
Client  AuthController  AuthService  MySQL (users)
                                       Redis (tokens)
                                       Gmail (emails)
`

## Security

- Passwords: BCrypt hashed
- Refresh tokens: SHA-256 hashed in Redis
- Cookies: HttpOnly + SameSite=Strict
