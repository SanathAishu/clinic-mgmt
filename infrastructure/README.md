# Hospital Management System - Infrastructure Setup

## Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Java 21 JDK
- Maven 3.9+

### Start Infrastructure

```bash
# From the project root directory
docker-compose up -d

# Check services are running
docker-compose ps

# View logs
docker-compose logs -f
```

### Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **PostgreSQL** | `localhost:5432` | User: `hospital_user`<br>Password: `hospital_password_2025`<br>Database: `hospital_db` |
| **Redis** | `localhost:6379` | Password: `redis_password_2025` |
| **RabbitMQ Management** | `http://localhost:15672` | User: `hospital_user`<br>Password: `rabbitmq_password_2025` |
| **RabbitMQ AMQP** | `localhost:5672` | Same as above |

### PostgreSQL Schemas

The following schemas are automatically created:

1. `auth_service` - Authentication and user management
2. `patient_service` - Patient demographics and records
3. `doctor_service` - Doctor profiles and specialties
4. `appointment_service` - Appointment scheduling
5. `medical_records_service` - Medical records, prescriptions, reports
6. `facility_service` - Room management and admissions
7. `notification_service` - Email notifications
8. `audit_service` - HIPAA compliance and audit logs

### Connect to PostgreSQL

```bash
# Using psql
docker exec -it hospital-postgres psql -U hospital_user -d hospital_db

# List schemas
\dn

# Switch to a schema
SET search_path TO patient_service;

# List tables in current schema
\dt
```

### Connect to Redis

```bash
# Using redis-cli
docker exec -it hospital-redis redis-cli -a redis_password_2025

# Test connection
PING

# View keys
KEYS *
```

### RabbitMQ Management UI

Open `http://localhost:15672` in your browser
- Username: `hospital_user`
- Password: `rabbitmq_password_2025`
- VHost: `hospital_vhost`

### Stop Infrastructure

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This deletes all data!)
docker-compose down -v
```

### Troubleshooting

**PostgreSQL not starting:**
```bash
# Check logs
docker-compose logs postgres

# Verify port is not in use
lsof -i :5432
```

**Redis password issues:**
```bash
# Connect without password first
docker exec -it hospital-redis redis-cli

# Then authenticate
AUTH redis_password_2025
```

**RabbitMQ not accessible:**
```bash
# Check if management plugin is enabled
docker exec -it hospital-rabbitmq rabbitmq-plugins list

# Restart container
docker-compose restart rabbitmq
```

### Health Checks

```bash
# Check PostgreSQL
docker exec hospital-postgres pg_isready -U hospital_user

# Check Redis
docker exec hospital-redis redis-cli -a redis_password_2025 ping

# Check RabbitMQ
docker exec hospital-rabbitmq rabbitmq-diagnostics ping
```

### Data Persistence

All data is persisted in Docker volumes:
- `postgres_data` - PostgreSQL database
- `redis_data` - Redis cache data
- `rabbitmq_data` - RabbitMQ messages and queues

### Production Considerations

1. **Change all passwords** in docker-compose.yml
2. Use **environment variables** or secrets management
3. Enable **SSL/TLS** for all connections
4. Setup **backup strategy** for PostgreSQL
5. Configure **Redis persistence** (AOF + RDB)
6. Enable **RabbitMQ clustering** for high availability
7. Use **external volumes** for production data
