# API Gateway

API Gateway for Hospital Management System with Vert.x routing, JWT validation, and dynamic service discovery.

## Features

- ✅ **Vert.x HTTP Routing** - High-performance async routing
- ✅ **SmallRye Stork + Consul** - Dynamic service discovery and load balancing
- ✅ **JWT Authentication** - Validates JWT tokens and extracts tenant context
- ✅ **Tenant Isolation** - Extracts `tenantId` from JWT and adds to headers
- ✅ **Rate Limiting** - Redis-based token bucket algorithm
- ✅ **Load Balancing** - Round-robin, random, least-requests strategies
- ✅ **Health Checks** - Monitors backend service health
- ✅ **CORS Support** - Cross-origin resource sharing for frontend
- ✅ **Loose Coupling** - Composition over inheritance pattern

## Architecture

```
Client Request
    ↓
[CORS Handler]
    ↓
[Body Parser]
    ↓
[Timeout Handler (30s)]
    ↓
[Rate Limiter (Redis)]
    ↓
[JWT Validator]
    ↓
    Extract:
    - tenantId → X-Tenant-Id header
    - userId → X-User-Id header
    - email → X-User-Email header
    - roles → X-User-Roles header
    - permissions → X-User-Permissions header
    ↓
[Stork Service Discovery]
    ↓
    Query Consul for service instances
    ↓
    Select healthy instance (load balancing)
    ↓
[Service Router]
    ↓
Backend Microservices (auto-discovered)
```

## Service Discovery

The gateway uses **SmallRye Stork + Consul** for dynamic service discovery:

- **No hardcoded URLs** - Services discovered via Consul
- **Dynamic instances** - New instances auto-discovered
- **Load balancing** - Round-robin across healthy instances
- **Health checks** - Only routes to healthy services
- **Zero-downtime** - Services can be added/removed without gateway restart

See [SERVICE_DISCOVERY.md](SERVICE_DISCOVERY.md) for detailed configuration options.

## Service Routing

| Path Prefix | Backend Service | Port |
|-------------|----------------|------|
| `/api/auth/**` | auth-service | 8081 |
| `/api/patients/**` | patient-service | 8082 |
| `/api/doctors/**` | doctor-service | 8083 |
| `/api/appointments/**` | appointment-service | 8084 |
| `/api/medical-records/**` | medical-records-service | 8085 |
| `/api/facilities/**` | facility-service | 8086 |
| `/api/notifications/**` | notification-service | 8087 |
| `/api/audit/**` | audit-service | 8088 |

## Public Paths (No Authentication)

- `/api/auth/login` - User login
- `/api/auth/register` - User registration
- `/q/health/**` - Health checks
- `/q/metrics` - Prometheus metrics
- `/swagger-ui/**` - API documentation

## Configuration

### Environment Variables

```properties
# Backend Services
services.auth-service.url=http://localhost:8081
services.patient-service.url=http://localhost:8082
# ... (see application.properties)

# JWT
mp.jwt.verify.publickey.location=/publicKey.pem
mp.jwt.verify.issuer=hospital-system

# Rate Limiting
gateway.rate-limit.enabled=true
gateway.rate-limit.requests-per-minute=100
gateway.rate-limit.burst=20

# Redis
quarkus.redis.hosts=redis://localhost:6379
```

## Running the Gateway

### Development Mode

```bash
cd api-gateway
./mvnw quarkus:dev
```

Gateway runs on `http://localhost:8080`

### Production (JVM)

```bash
./mvnw clean package
java -jar target/quarkus-app/quarkus-run.jar
```

### Production (Native)

```bash
./mvnw package -Pnative
./target/api-gateway-1.0.0-runner
```

**Native startup:** ~0.5s (vs ~3s JVM)
**Native memory:** ~50MB (vs ~150MB JVM)

## Example Requests

### 1. Login (Public Path)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "doctor@hospital.com",
    "password": "password123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "uuid",
    "email": "doctor@hospital.com",
    "role": "DOCTOR"
  }
}
```

### 2. Get Patients (Authenticated)

```bash
TOKEN="your-jwt-token"

curl -X GET http://localhost:8080/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

The gateway will:
1. Validate JWT
2. Extract `tenantId` from JWT
3. Add `X-Tenant-Id: hosp-001` header
4. Forward to patient-service with all headers

### 3. Check Health

```bash
curl http://localhost:8080/q/health
```

Response:
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "backend-services",
      "status": "UP",
      "data": {
        "auth-service": "UP",
        "patient-service": "UP",
        "doctor-service": "UP",
        "appointment-service": "UP"
      }
    }
  ]
}
```

## Headers Forwarded to Backend Services

The gateway adds these headers to all authenticated requests:

- `Authorization: Bearer <token>` - Original JWT token
- `X-Tenant-Id: <tenantId>` - Extracted from JWT (CRITICAL for multi-tenancy)
- `X-User-Id: <userId>` - User UUID from JWT subject
- `X-User-Email: <email>` - User email
- `X-User-Roles: <roles>` - Comma-separated roles (e.g., "DOCTOR,DEPARTMENT_HEAD")
- `X-User-Permissions: <permissions>` - Comma-separated permissions
- `X-Request-Id: <uuid>` - Unique request ID for tracing

Backend services **MUST NOT trust `tenantId` from request body** - only use `X-Tenant-Id` header.

## Rate Limiting

- **Limit:** 100 requests per minute per user/IP
- **Burst:** 20 requests can be made immediately
- **Storage:** Redis (keys expire after 60 seconds)
- **Response:** `429 Too Many Requests` with `Retry-After: 60` header

Rate limit headers in response:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
```

## Security

### JWT Validation

1. Extracts `Authorization: Bearer <token>` header
2. Parses and validates JWT signature
3. Checks issuer (`hospital-system`)
4. Validates expiration
5. Extracts required claims (`tenantId`, `userId`, `email`, `roles`)

### Tenant Isolation

**CRITICAL:** All backend services MUST:
1. Read `tenantId` from `X-Tenant-Id` header
2. Filter ALL database queries by `tenantId`
3. NEVER trust `tenantId` from request body

The gateway ensures `X-Tenant-Id` header is set from validated JWT.

## Monitoring

### Health Checks

- **Liveness:** `GET /q/health/live` - Gateway process is alive
- **Readiness:** `GET /q/health/ready` - Gateway + backend services ready
- **Full Health:** `GET /q/health` - Detailed health status

### Metrics

Prometheus metrics available at `/q/metrics`:

- `http_server_requests_total` - Total requests
- `http_server_requests_duration` - Request duration histogram
- `gateway_route_errors_total` - Routing errors
- `gateway_rate_limit_exceeded_total` - Rate limit violations

## Error Handling

| Status Code | Error | Description |
|-------------|-------|-------------|
| 400 | Bad Request | Invalid request body |
| 401 | Unauthorized | Missing/invalid JWT token |
| 403 | Forbidden | Valid token but insufficient permissions |
| 404 | Not Found | Service/route not found |
| 429 | Too Many Requests | Rate limit exceeded |
| 503 | Service Unavailable | Backend service down |
| 504 | Gateway Timeout | Backend service timeout (30s) |

## Development Tips

### Testing with cURL

```bash
# Login and save token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"doctor@hospital.com","password":"password123"}' \
  | jq -r '.token')

# Use token for authenticated requests
curl -X GET http://localhost:8080/api/patients \
  -H "Authorization: Bearer $TOKEN" \
  | jq
```

### Viewing Logs

```bash
# All logs
./mvnw quarkus:dev

# Debug level
./mvnw quarkus:dev -Dquarkus.log.category.\"com.hospital\".level=DEBUG
```

## Loose Coupling Design

The gateway uses **composition over inheritance**:

```java
// ✅ GOOD: Filters are injected, not extended
@ApplicationScoped
public class GatewayRoutes {
    @Inject JwtAuthFilter jwtAuthFilter;      // Composition
    @Inject RateLimitFilter rateLimitFilter;  // Composition
    @Inject ServiceRouter serviceRouter;      // Composition

    // Chain filters via Vert.x routing
    router.route("/api/*").handler(ctx -> {
        if (!rateLimitFilter.checkRateLimit(ctx)) return;
        if (!jwtAuthFilter.authenticate(ctx)) return;
        serviceRouter.route(ctx);
    });
}
```

No inheritance hierarchies, easy to test and maintain.

## Next Steps

- [ ] Add circuit breaker for backend service failures
- [ ] Add request/response caching
- [ ] Add distributed tracing (OpenTelemetry)
- [ ] Add WebSocket support for real-time features
- [ ] Add GraphQL gateway (optional)
