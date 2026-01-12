# 📬 Postman Testing Guide - ChatApp Authentication API

This guide explains how to test every authentication endpoint using Postman.

---

## 📋 Prerequisites (Before Testing)

### 1. Start Required Services

Make sure these are running before testing:

**A. Start MySQL Server:**
```powershell
# Windows: Start from Services
net start MySQL80

# Verify MySQL is running
mysql -u root -p -e "SELECT 1"
```

**B. Start Docker Desktop (Required for Redis):**
```powershell
# Option 1: Click "Docker Desktop" in Windows Start Menu
# Option 2: Run this command
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Wait 30-60 seconds, then verify Docker is running
docker info
```

**C. Start Redis in Docker:**
```powershell
# Check if Redis container already exists
docker ps -a | findstr redis

# If no container exists, create one:
docker run --name redis-chatapp -p 6379:6379 -d redis:latest

# If container exists but stopped, start it:
docker start redis-chatapp

# Verify Redis is working
docker exec redis-chatapp redis-cli ping
# Expected output: PONG
```

**D. Start Spring Boot Application:**
```powershell
cd backend
mvn spring-boot:run
# Or run ChatAppApplication.java from your IDE
```

### 2. Verify Services Are Running

| Service | Check Command | Expected |
|---------|--------------|----------|
| MySQL | `mysql -u root -p -e "SELECT 1"` | Returns 1 |
| Docker | `docker info` | Shows Docker version info |
| Redis | `docker exec redis-chatapp redis-cli ping` | PONG |
| Backend | Open `http://localhost:8080/actuator/health` | `{"status":"UP"}` |

⚠️ **IMPORTANT:** If you skip Redis setup, you will get "Unable to connect to Redis" error when calling ANY API. See the **Redis Troubleshooting** section at the bottom of this guide for detailed fix instructions.

---

## 🛠️ Postman Setup

### Step 1: Download & Install Postman
- Download from: https://www.postman.com/downloads/
- Install and create a free account (optional)

### Step 2: Create a New Collection
1. Click **Collections** in left sidebar
2. Click **+** to create new collection
3. Name it: `ChatApp Auth API`

### Step 3: Create Environment Variables
1. Click **Environments** (gear icon)
2. Click **+** to create new environment
3. Name it: `ChatApp Local`
4. Add these variables:

| Variable | Initial Value | Current Value |
|----------|---------------|---------------|
| `base_url` | `http://localhost:8080` | `http://localhost:8080` |
| `access_token` | (leave empty) | (leave empty) |
| `refresh_token` | (leave empty) | (leave empty) |

5. Click **Save**
6. Select this environment from dropdown (top-right)

---

## 🧪 API Testing - Step by Step

---

### 📝 TEST 1: User Registration

**Purpose:** Create a new user account and trigger verification email.

#### Request Setup:
- **Method:** `POST`
- **URL:** `{{base_url}}/api/auth/register`
- **Headers:**
  | Key | Value |
  |-----|-------|
  | Content-Type | application/json |

- **Body** (raw JSON):
```json
{
    "email": "testuser@example.com",
    "password": "SecurePass123!",
    "displayName": "Test User"
}
```

#### How to Send:
1. Create new request in your collection
2. Set method to `POST`
3. Enter URL: `{{base_url}}/api/auth/register`
4. Go to **Headers** tab, add `Content-Type: application/json`
5. Go to **Body** tab, select **raw**, select **JSON**
6. Paste the JSON above
7. Click **Send**

#### Expected Responses:

✅ **Success (200 OK):**
```
(empty body)
```
- Check your email inbox for verification link!

❌ **Email Already Exists (409 Conflict):**
```json
{
    "timestamp": "2026-01-04T10:30:00",
    "status": 409,
    "error": "Conflict",
    "message": "Email already registered"
}
```

❌ **Rate Limit Exceeded (429 Too Many Requests):**
```json
{
    "error": "Too many requests. Please try again later."
}
```

#### What Happens Behind the Scenes:
1. Server checks if email exists in MySQL → throws 409 if exists
2. Password is hashed with BCrypt (salt is auto-generated)
3. User saved to MySQL with status `PENDING_VERIFICATION`
4. UUID verification token created and stored in Redis (24hr TTL)
5. Email sent with verification link

---

### 📧 TEST 2: Email Verification

**Purpose:** Activate user account by clicking verification link.

#### Option A: Click Email Link (Recommended)
1. Check your inbox for email from `piranaberem14@gmail.com`
2. Click the verification link
3. You'll see a success page in your browser

#### Option B: Test via Postman
- **Method:** `GET`
- **URL:** `{{base_url}}/api/auth/verify?token=YOUR_TOKEN_HERE`

To get the token:
1. Check the server console logs
2. Look for: `Verification token created in Redis for user: testuser@example.com`
3. Or check Redis: `redis-cli keys "verification:*"`

#### Expected Responses:

✅ **Success (200 OK):** HTML page with "Email Verified Successfully!"

❌ **Invalid/Expired Token (400 Bad Request):** HTML page with "Verification Failed"

---

### 🔐 TEST 3: User Login

**Purpose:** Authenticate and receive JWT tokens.

#### Request Setup:
- **Method:** `POST`
- **URL:** `{{base_url}}/api/auth/login`
- **Headers:**
  | Key | Value |
  |-----|-------|
  | Content-Type | application/json |

- **Body** (raw JSON):
```json
{
    "email": "testuser@example.com",
    "password": "SecurePass123!"
}
```

#### Expected Responses:

✅ **Success (200 OK):**
```json
{
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlckBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE3MDQzNjQ2MDAsImV4cCI6MTcwNDM2NTUwMH0.abc123...",
    "refreshToken": null
}
```

**IMPORTANT:** Look at the **Cookies** tab in Postman response!
You should see a cookie named `refreshToken` with the actual refresh token value.

#### Save Token for Later Tests:
1. Go to **Tests** tab in Postman
2. Add this script:
```javascript
// Automatically save access token to environment variable
var jsonData = pm.response.json();
if (jsonData.accessToken) {
    pm.environment.set("access_token", jsonData.accessToken);
    console.log("Access token saved!");
}
```

#### Error Responses:

❌ **Invalid Credentials (400 Bad Request):**
```json
{
    "timestamp": "2026-01-04T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Invalid credentials"
}
```

❌ **Email Not Verified (400 Bad Request):**
```json
{
    "timestamp": "2026-01-04T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Email not verified. Please verify your email before logging in."
}
```

---

### 🔄 TEST 4: Refresh Access Token

**Purpose:** Get a new access token without re-entering password.

#### When to Use:
- When access token expires (after 15 minutes)
- When API returns 401 Unauthorized

#### Request Setup:
- **Method:** `POST`
- **URL:** `{{base_url}}/api/auth/refresh`
- **Headers:**
  | Key | Value |
  |-----|-------|
  | Content-Type | application/json |

#### Important: Cookie Must Be Sent!
Postman should automatically send the `refreshToken` cookie if you logged in previously.

To verify cookies are being sent:
1. Go to **Cookies** (under Send button)
2. Check that `refreshToken` exists for `localhost`

#### Alternative: Send Token in Body
If cookies don't work, send the refresh token in the body:
```
(raw text - NOT JSON)
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlckBleGFtcGxlLmNvbSIsImlhdCI6MTcwNDM2NDYwMCwiZXhwIjoxNzA2OTU2NjAwfQ.xyz789...
```

#### Expected Response:

✅ **Success (200 OK):**
```json
{
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.NEW_TOKEN_HERE...",
    "refreshToken": null
}
```

❌ **Invalid/Expired Refresh Token (401 Unauthorized):**
```json
{
    "timestamp": "2026-01-04T10:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid refresh token"
}
```

---

### 🔒 TEST 5: Access Protected Endpoint

**Purpose:** Verify that JWT authentication works.

#### Request Setup:
- **Method:** `GET`
- **URL:** `{{base_url}}/api/protected/test` (or any protected endpoint)
- **Headers:**
  | Key | Value |
  |-----|-------|
  | Authorization | Bearer {{access_token}} |

**Note:** Replace `{{access_token}}` with your actual token, or use environment variable if set up.

#### Expected Responses:

✅ **With Valid Token:** Endpoint response (depends on endpoint)

❌ **Without Token (401/403):**
```json
{
    "timestamp": "2026-01-04T10:30:00",
    "status": 403,
    "error": "Forbidden",
    "message": "Access Denied"
}
```

❌ **With Expired Token (401):**
```json
{
    "timestamp": "2026-01-04T10:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Token expired"
}
```

---

### 🚪 TEST 6: User Logout

**Purpose:** Invalidate refresh token and end session.

> ⚠️ **IMPORTANT:** Logout endpoint requires authentication! You must include the Bearer token in the Authorization header.

#### Request Setup:
- **Method:** `POST`
- **URL:** `{{base_url}}/api/auth/logout`
- **Headers:**
  | Key | Value |
  |-----|-------|
  | Content-Type | application/json |
  | Authorization | Bearer {{accessToken}} |

(Cookie will be sent automatically)

#### Expected Response:

✅ **Success (200 OK):**
```
(empty body)
```

❌ **If No Token Provided (401 Unauthorized):**
```json
{
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource"
}
```

#### Verify Logout Worked:
1. Try to refresh token → Should get 401 error
2. Check Redis: `redis-cli keys "refresh:*"` → Token should be deleted
3. Check Cookies in Postman → `refreshToken` should be cleared

---

### 🚫 TEST 7: Rate Limiting

**Purpose:** Verify that rate limiting protects against abuse.

#### Test Rate Limiting:
1. Send login request with wrong password
2. Repeat rapidly 6+ times within 1 minute
3. After 5 attempts, you should get:

```json
{
    "error": "Too many requests. Please try again later."
}
```
**Status:** 429 Too Many Requests

#### Rate Limits:
| Endpoint | Limit | Reset |
|----------|-------|-------|
| `/api/auth/login` | 5/minute | Every minute |
| `/api/auth/register` | 3/minute | Every minute |
| Other endpoints | 100/minute | Every minute |

---

## 📊 Complete Test Sequence

Run these tests in order for a complete flow:

| # | Test | Expected Result |
|---|------|-----------------|
| 1 | Register new user | 200 OK, email sent |
| 2 | Try login (before verify) | 400 - Email not verified |
| 3 | Verify email (click link) | Success page |
| 4 | Login | 200 OK, tokens received |
| 5 | Access protected endpoint | Success |
| 6 | Wait 15+ min or use old token | 401 Unauthorized |
| 7 | Refresh token | 200 OK, new access token |
| 8 | Access protected endpoint | Success |
| 9 | Logout | 200 OK |
| 10 | Try refresh after logout | 401 - Invalid token |

---

## 🔧 Troubleshooting

### "Connection Refused"
- ✅ Check Spring Boot is running on port 8080
- ✅ Check no firewall blocking the port

### "500 Internal Server Error"
- ✅ Check MySQL is running
- ✅ Check Redis is running (see Redis Troubleshooting below)
- ✅ Check server console for stack trace

### "Email Not Received"
- ✅ Check spam folder
- ✅ Check Gmail app password is correct
- ✅ Check server logs for email errors

### "Token Invalid" After Login
- ✅ Make sure you're using the token from the response
- ✅ Include "Bearer " prefix (with space)
- ✅ Check token hasn't expired (15 min for access)

### Cookies Not Working
- ✅ Go to Postman Settings → Cookies → Enable
- ✅ Check "Interceptor" is enabled
- ✅ Try disabling "Send cookies" and sending in body instead

---

## 🔴 REDIS TROUBLESHOOTING (IMPORTANT!)

### Why Does ChatApp Need Redis?

ChatApp uses Redis (a fast in-memory database) to store:
1. **Email Verification Tokens** - Temporary tokens sent in verification emails
2. **Refresh Tokens** - Long-lived tokens for session management  
3. **Rate Limit Counters** - Tracks how many requests each IP makes

Without Redis running, you will get this error when calling ANY API:
```
Unable to connect to Redis
```

### Why Do I Get "Unable to connect to Redis" Error?

This error happens when your Spring Boot application cannot talk to Redis. Common reasons:

| Problem | Why It Happens |
|---------|----------------|
| Docker Desktop not running | Redis runs inside Docker, Docker must be open |
| Redis container stopped | Container exists but is not running |
| Redis container deleted | You need to create a new container |
| Port 6379 blocked/used | Another program is using Redis port |
| Container crashed | Check logs to see what went wrong |

### Step-by-Step: Fix Redis Connection Error

**STEP 1: Check if Docker Desktop is Running**

Open PowerShell and run:
```powershell
docker info
```

If you see an error like:
```
error during connect: This error may indicate that the docker daemon is not running
```

**FIX:** Start Docker Desktop manually:
- Find "Docker Desktop" in Windows Start Menu and click it
- OR run this command:
```powershell
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
```
- Wait 30-60 seconds for Docker to fully start
- Try `docker info` again - should show system info without errors

---

**STEP 2: Check if Redis Container Exists**

Run this command to see ALL containers (running and stopped):
```powershell
docker ps -a
```

Look for a container with `redis` in the IMAGE column.

**CASE A - No Redis container found:**
Create one:
```powershell
docker run --name redis-chatapp -p 6379:6379 -d redis:latest
```

**CASE B - Redis container exists but STATUS shows "Exited":**
Start the existing container:
```powershell
docker start redis-chatapp
```

**CASE C - Redis container shows "Up" but still getting error:**
Continue to Step 3.

---

**STEP 3: Verify Redis is Actually Working**

Test Redis connection from inside the container:
```powershell
docker exec redis-chatapp redis-cli ping
```

**Expected output:** `PONG`

If you get `PONG`, Redis is working. Try your API again.

If you get an error, the container may be corrupted. Remove and recreate it:
```powershell
docker rm -f redis-chatapp
docker run --name redis-chatapp -p 6379:6379 -d redis:latest
```

---

**STEP 4: Check if Port 6379 is Available**

If Redis container is running but you still get connection errors, check if port 6379 is free:
```powershell
netstat -ano | findstr :6379
```

If you see another process using port 6379:
1. Find the process ID (last column in output)
2. Kill it: `taskkill /PID <process_id> /F`
3. Or use a different port for Redis:
```powershell
docker rm -f redis-chatapp
docker run --name redis-chatapp -p 6380:6379 -d redis:latest
```
(Then update application.properties to use port 6380)

---

**STEP 5: Check Redis Container Logs**

If Redis keeps crashing, check the logs:
```powershell
docker logs redis-chatapp
```

Common issues:
- Memory errors: Your system might be low on RAM
- Permission errors: Try running PowerShell as Administrator

---

### Quick Reference: All Redis Commands

```powershell
# ===== DOCKER DESKTOP =====
# Start Docker Desktop (if not running)
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Check if Docker is running
docker info

# ===== REDIS CONTAINER =====
# See all containers (running + stopped)
docker ps -a

# See only running containers
docker ps

# Create new Redis container
docker run --name redis-chatapp -p 6379:6379 -d redis:latest

# Start stopped container
docker start redis-chatapp

# Stop running container
docker stop redis-chatapp

# Remove container (must be stopped first, or use -f)
docker rm redis-chatapp
docker rm -f redis-chatapp

# ===== TESTING REDIS =====
# Test if Redis responds
docker exec redis-chatapp redis-cli ping

# Open Redis command line
docker exec -it redis-chatapp redis-cli

# Inside Redis CLI, useful commands:
#   KEYS *              (show all keys)
#   KEYS verification:* (show verification tokens)
#   KEYS refresh:*      (show refresh tokens)
#   GET <key>           (get value of a key)
#   DEL <key>           (delete a key)
#   FLUSHALL            (delete everything - use carefully!)
#   EXIT                (leave Redis CLI)

# ===== TROUBLESHOOTING =====
# View Redis logs
docker logs redis-chatapp

# View last 50 lines of logs
docker logs --tail 50 redis-chatapp

# Check what's using port 6379
netstat -ano | findstr :6379

# Check Redis container details
docker inspect redis-chatapp
```

---

### Still Not Working? Complete Reset

If nothing works, do a complete reset:

```powershell
# 1. Stop and remove Redis container
docker rm -f redis-chatapp

# 2. Remove Redis image (optional, for fresh download)
docker rmi redis:latest

# 3. Pull fresh Redis image
docker pull redis:latest

# 4. Create new container
docker run --name redis-chatapp -p 6379:6379 -d redis:latest

# 5. Verify it works
docker exec redis-chatapp redis-cli ping
```

If you get `PONG`, restart your Spring Boot application and try the API again.

---

### Understanding the Architecture

```
When you call POST /api/auth/register:

[Postman] 
    |
    v
[Spring Boot App] (localhost:8080)
    |
    +--> [MySQL] (localhost:3306) - Saves user permanently
    |
    +--> [Redis] (localhost:6379) - Saves verification token (expires in 24h)
    |
    +--> [Gmail SMTP] - Sends verification email

If Redis is not running, the app cannot save the verification token,
so it throws "Unable to connect to Redis" error.
The user might still be saved to MySQL (check your database).
```

```
Token Storage Strategy:

+------------------+-------------------+------------------+
|     Data         |   Where Stored    |   Why There      |
+------------------+-------------------+------------------+
| User account     | MySQL             | Permanent data   |
| Password (hash)  | MySQL             | Permanent data   |
| Access Token     | Client memory     | Short-lived      |
| Refresh Token    | Redis + Cookie    | Can be revoked   |
| Verify Token     | Redis             | Expires in 24h   |
| Rate Limit Count | Redis             | Resets each min  |
+------------------+-------------------+------------------+
```

---

## 📁 Postman Collection Export

You can import a pre-configured collection. Create a new file `ChatApp_Auth.postman_collection.json`:

```json
{
    "info": {
        "name": "ChatApp Auth API",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "1. Register",
            "request": {
                "method": "POST",
                "url": "{{base_url}}/api/auth/register",
                "header": [{"key": "Content-Type", "value": "application/json"}],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"email\": \"testuser@example.com\",\n    \"password\": \"SecurePass123!\",\n    \"displayName\": \"Test User\"\n}"
                }
            }
        },
        {
            "name": "2. Login",
            "request": {
                "method": "POST",
                "url": "{{base_url}}/api/auth/login",
                "header": [{"key": "Content-Type", "value": "application/json"}],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"email\": \"testuser@example.com\",\n    \"password\": \"SecurePass123!\"\n}"
                }
            }
        },
        {
            "name": "3. Refresh Token",
            "request": {
                "method": "POST",
                "url": "{{base_url}}/api/auth/refresh",
                "header": [{"key": "Content-Type", "value": "application/json"}]
            }
        },
        {
            "name": "4. Logout",
            "request": {
                "method": "POST",
                "url": "{{base_url}}/api/auth/logout",
                "header": [{"key": "Content-Type", "value": "application/json"}]
            }
        }
    ]
}
```

---

## ✅ Checklist: Section 1 Verification

Use this checklist to verify all features are working:

- [ ] **Registration:** New users can register with email/password
- [ ] **Email Verification:** Verification emails are sent and links work
- [ ] **Login:** Valid credentials return JWT tokens
- [ ] **JWT Validation:** Protected endpoints require valid tokens
- [ ] **Token Refresh:** New access tokens can be obtained with refresh token
- [ ] **Logout:** Sessions can be properly terminated
- [ ] **Password Security:** Passwords are BCrypt hashed (check MySQL)
- [ ] **Rate Limiting:** Brute-force protection works (429 after limits)
- [ ] **Error Handling:** Proper HTTP status codes (401, 403, 409, 429)
- [ ] **Logging:** Authentication events are logged

---

**Happy Testing! 🎉**
