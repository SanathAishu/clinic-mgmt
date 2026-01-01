# Infrastructure Setup

## Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Java 21 JDK
- Maven 3.9+

### Start Infrastructure Only

```bash
# From the project root directory
docker-compose -f docker-compose-infra.yml up -d

# Check services are running
docker-compose -f docker-compose-infra.yml ps

# View logs
docker-compose -f docker-compose-infra.yml logs -f
```

### Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **PostgreSQL** | `localhost:5432` | User: `postgres`, Password: `postgres` |
| **Redis** | `localhost:6379` | No password (local dev) |
| **RabbitMQ Management** | http://localhost:15672 | User: `guest`, Password: `guest` |
| **RabbitMQ AMQP** | `localhost:5672` | Same as above |

### PostgreSQL Databases

The init script creates 8 databases (one per microservice):

1. `auth_service`
2. `patient_service`
3. `doctor_service`
4. `appointment_service`
5. `medical_records_service`
6. `facility_service`
7. `notification_service`
8. `audit_service`

### Connect to PostgreSQL

```bash
# Using psql
docker exec -it hospital-postgres psql -U postgres

# List databases
\l

# Connect to a database
\c patient_service

# List tables
\dt
```

### Connect to Redis

```bash
# Using redis-cli
docker exec -it hospital-redis redis-cli

# Test connection
PING

# View keys
KEYS *
```

### RabbitMQ Management UI

Open http://localhost:15672 in your browser
- Username: `guest`
- Password: `guest`

### Stop Infrastructure

```bash
# Stop all services
docker-compose -f docker-compose-infra.yml down

# Stop and remove volumes (WARNING: Deletes all data!)
docker-compose -f docker-compose-infra.yml down -v
```

### Health Checks

```bash
# Check PostgreSQL
docker exec hospital-postgres pg_isready -U postgres

# Check Redis
docker exec hospital-redis redis-cli ping

# Check RabbitMQ
docker exec hospital-rabbitmq rabbitmq-diagnostics ping
```

### Data Persistence

All data is persisted in Docker volumes:
- `postgres_data` - PostgreSQL databases
- `redis_data` - Redis cache data
- `rabbitmq_data` - RabbitMQ messages and queues

### Production Considerations

1. **Change all passwords** - Use strong, unique passwords
2. **Use environment variables** - Never commit secrets
3. **Enable SSL/TLS** - For all connections
4. **Setup backups** - Especially for PostgreSQL
5. **Configure Redis persistence** - AOF + RDB recommended
6. **RabbitMQ clustering** - For high availability
