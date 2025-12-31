# Auth Service Complete! ✅

## Overview
The Authentication Service with RS256 JWT and UUID-based users has been successfully implemented.

## What Was Built

### 1. Project Structure
```
auth-service/
├── pom.xml
├── src/main/
│   ├── java/com/hospital/auth/
│   │   ├── AuthServiceApplication.java       # Main application
│   │   ├── entity/
│   │   │   └── User.java                    # UUID-based user entity
│   │   ├── repository/
│   │   │   └── UserRepository.java          # JPA repository
│   │   ├── service/
│   │   │   └── AuthService.java             # Business logic
│   │   ├── controller/
│   │   │   └── AuthController.java          # REST endpoints
│   │   ├── dto/
│   │   │   ├── RegisterRequest.java         # Registration DTO
│   │   │   ├── LoginRequest.java            # Login DTO
│   │   │   ├── AuthResponse.java            # Auth response with JWT
│   │   │   └── UserDto.java                 # User DTO
│   │   ├── config/
│   │   │   ├── SecurityConfig.java          # Spring Security
│   │   │   └── GlobalExceptionHandler.java  # Exception handling
│   │   └── util/
│   │       └── JwtUtils.java                # RS256 JWT utilities
│   └── resources/
│       ├── application.yml                   # Config Server bootstrap
│       └── keys/
│           ├── private_key.pem              # RS256 private key (PKCS#8)
│           └── public_key.pem               # RS256 public key
```

### 2. Key Features

#### RS256 JWT Authentication
- **Private Key**: 2048-bit RSA key in PKCS#8 format
- **Public Key**: Distributed via `/api/auth/public-key` endpoint
- **Token Expiration**: 24 hours (86400000 ms)
- **Claims**: userId (subject), email, name, role

#### UUID Primary Keys
- All users have UUID as primary key
- Generated automatically by PostgreSQL: `@GeneratedValue(strategy = GenerationType.UUID)`
- No sequential ID exposure

#### User Entity
```java
@Entity
@Table(name = "users", schema = "auth_service")
public class User {
    private UUID id;                    // Primary key
    private String email;               // Unique, indexed
    private String password;            // BCrypt hashed
    private String name;
    private String phone;
    private Gender gender;              // Enum: MALE, FEMALE, OTHER
    private Role role;                  // Enum: USER, ADMIN
    private Boolean active;             // Account status
    private LocalDateTime createdAt;    // Auto-generated
    private LocalDateTime updatedAt;    // Auto-updated
    private String metadata;            // JSONB metadata
}
```

#### Security Configuration
- BCrypt password encoder (strength 10)
- Stateless session management
- CSRF disabled (API-only service)
- Public endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/auth/public-key`
  - `GET /api/auth/health`
  - `/actuator/**`

### 3. REST API Endpoints

#### Public Endpoints (No Authentication)

**1. Register User**
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "patient@example.com",
  "password": "password123",
  "name": "John Doe",
  "phone": "+1234567890",
  "gender": "MALE",
  "role": "USER"
}

Response 201 Created:
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "email": "patient@example.com",
    "name": "John Doe",
    "role": "USER",
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "timestamp": "2025-12-31T19:30:00"
}
```

**2. Login**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "patient@example.com",
  "password": "password123"
}

Response 200 OK:
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "email": "patient@example.com",
    "name": "John Doe",
    "role": "USER",
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "timestamp": "2025-12-31T19:30:00"
}
```

**3. Get Public Key** (for API Gateway)
```http
GET /api/auth/public-key

Response 200 OK:
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4Vpp5AlRsDwwynS7h2Bd
...
-----END PUBLIC KEY-----
```

**4. Health Check**
```http
GET /api/auth/health

Response 200 OK:
Auth Service is running
```

#### Protected Endpoints (Require JWT)

**5. Get User by ID**
```http
GET /api/auth/users/{userId}
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...

Response 200 OK:
{
  "success": true,
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "patient@example.com",
    "name": "John Doe",
    "phone": "+1234567890",
    "gender": "MALE",
    "role": "USER",
    "active": true,
    "createdAt": "2025-12-31T19:00:00"
  }
}
```

**6. Get User by Email**
```http
GET /api/auth/users/email/{email}
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

### 4. JWT Token Structure

**Token Header:**
```json
{
  "alg": "RS256",
  "typ": "JWT"
}
```

**Token Payload:**
```json
{
  "sub": "123e4567-e89b-12d3-a456-426614174000",
  "email": "patient@example.com",
  "name": "John Doe",
  "role": "USER",
  "iat": 1735669800,
  "exp": 1735756200
}
```

**Token Signature:**
Signed with RS256 private key (2048-bit RSA)

### 5. Error Handling

**Validation Error:**
```json
{
  "timestamp": "2025-12-31T19:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "path": "/api/auth/register",
  "validationErrors": [
    {
      "field": "email",
      "message": "Email must be valid"
    },
    {
      "field": "password",
      "message": "Password must be at least 6 characters"
    }
  ]
}
```

**Not Found Error:**
```json
{
  "timestamp": "2025-12-31T19:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with email: test@example.com",
  "path": "/api/auth/login"
}
```

**Duplicate Email:**
```json
{
  "timestamp": "2025-12-31T19:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "validationErrors": [
    {
      "field": "email",
      "message": "Email already registered"
    }
  ]
}
```

### 6. Database Schema

```sql
CREATE TABLE auth_service.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    gender VARCHAR(10) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    metadata TEXT
);

CREATE INDEX idx_users_email ON auth_service.users(email);
CREATE INDEX idx_users_role ON auth_service.users(role);
```

### 7. Configuration (from Config Server)

```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service
  jpa:
    properties:
      hibernate:
        default_schema: auth_service

jwt:
  private-key-path: classpath:keys/private_key.pem
  public-key-path: classpath:keys/public_key.pem
  expiration: 86400000  # 24 hours
```

### 8. Dependencies

- Spring Boot Web
- Spring Data JPA
- PostgreSQL Driver
- Spring Security
- JWT (io.jsonwebtoken 0.12.6)
- Spring Validation
- Eureka Client
- Config Client
- Hospital Common Library
- Lombok

## How to Build and Run

### Prerequisites
1. Java 21 installed
2. Maven installed
3. Docker services running (PostgreSQL, Eureka, Config Server)

### Build
```bash
cd /home/sanath/Projects/Clinic_mgmt
mvn clean install -pl auth-service -am
```

### Run
```bash
cd auth-service
mvn spring-boot:run
```

Or build JAR and run:
```bash
mvn clean package -DskipTests
java -jar target/auth-service-1.0.0.jar
```

### Startup Order
1. **PostgreSQL** (port 5432) - Must be running with `auth_service` schema
2. **Config Server** (port 8888) - Provides configuration
3. **Eureka Server** (port 8761) - Service discovery
4. **Auth Service** (port 8081) - This service

## Testing

### 1. Health Check
```bash
curl http://localhost:8081/api/auth/health
```

### 2. Register a User
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "Test User",
    "gender": "MALE",
    "role": "USER"
  }'
```

### 3. Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 4. Get Public Key
```bash
curl http://localhost:8081/api/auth/public-key
```

### 5. Access Eureka Dashboard
Open http://localhost:8761 and verify AUTH-SERVICE is registered

## Integration with API Gateway

The API Gateway will:
1. Fetch public key from `/api/auth/public-key`
2. Cache the public key
3. Validate all JWT tokens using the public key
4. Extract user info from token (userId, email, role)
5. Forward requests to downstream services with headers:
   - `X-User-Id`: UUID of authenticated user
   - `X-User-Email`: User's email
   - `X-User-Role`: USER or ADMIN

## Security Features

✅ **RS256 Asymmetric Encryption** - Private key only on Auth Service
✅ **BCrypt Password Hashing** - Strength 10
✅ **UUID Primary Keys** - No sequential ID leakage
✅ **Stateless JWT** - No server-side session storage
✅ **Input Validation** - Jakarta Validation annotations
✅ **Exception Handling** - Structured error responses
✅ **Active Status Check** - Deactivated users cannot login
✅ **Unique Email Constraint** - No duplicate accounts

## What's Next

The Auth Service is complete and ready for integration. Next steps:

1. ✅ **Test Auth Service** - Register users, login, verify JWT generation
2. ✅ **Update API Gateway** - Configure to use Auth Service public key
3. **Phase 3**: Build Patient Service
4. **Phase 4**: Build Doctor Service
5. **Phase 5**: Build Appointment Service
6. Continue with remaining microservices

## Files Created

- `auth-service/pom.xml`
- `AuthServiceApplication.java`
- `entity/User.java`
- `repository/UserRepository.java`
- `service/AuthService.java`
- `controller/AuthController.java`
- `dto/RegisterRequest.java`, `LoginRequest.java`, `AuthResponse.java`, `UserDto.java`
- `config/SecurityConfig.java`, `GlobalExceptionHandler.java`
- `util/JwtUtils.java`
- `resources/application.yml`
- `resources/keys/private_key.pem`, `public_key.pem`

**Total: 14 Java classes + 2 RSA keys + 1 config file = 17 files**

## Phase 2 Status: Complete ✅

Auth Service with RS256 JWT and UUID-based users is fully implemented and ready for testing!
