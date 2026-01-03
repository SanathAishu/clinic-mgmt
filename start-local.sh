#!/bin/bash

##############################################################################
# Startup Script for Hospital Management System (Quarkus + Infrastructure)
#
# Starts all required services in the correct order:
# 1. Infrastructure containers (PostgreSQL, Redis, RabbitMQ, Consul)
# 2. Prometheus and Grafana (optional monitoring)
# 3. Quarkus microservices (auth-service, audit-service, api-gateway)
##############################################################################

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Starting Hospital Management System                          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"

# ============================================================================
# Step 1: Start Infrastructure Containers
# ============================================================================
echo -e "\n${YELLOW}[1/4] Starting infrastructure containers...${NC}"

cd "$PROJECT_DIR/docker"
# Try to start existing containers first, then create if needed
docker-compose start postgres redis rabbitmq consul 2>/dev/null || docker-compose up -d postgres redis rabbitmq consul
echo -e "${GREEN}✓ PostgreSQL, Redis, RabbitMQ, Consul started${NC}"

# Wait for services to be healthy
echo "Waiting for infrastructure to be ready..."
sleep 5

# ============================================================================
# Step 2: Start Monitoring Stack (Optional)
# ============================================================================
echo -e "\n${YELLOW}[2/4] Starting monitoring stack (Prometheus, Grafana)...${NC}"

docker-compose up -d prometheus grafana
echo -e "${GREEN}✓ Prometheus (http://localhost:9090) and Grafana (http://localhost:3000) started${NC}"

cd "$PROJECT_DIR"

# ============================================================================
# Step 3: Start Quarkus Microservices
# ============================================================================
echo -e "\n${YELLOW}[3/4] Starting Quarkus microservices...${NC}"

# Auth Service
echo "Starting auth-service on port 8081..."
./gradlew :auth-service:quarkusDev > /tmp/auth-service.log 2>&1 &
AUTH_SERVICE_PID=$!
echo "Auth-service PID: $AUTH_SERVICE_PID"

# Wait for auth-service to start
sleep 8

# Audit Service
echo "Starting audit-service on port 8088..."
./gradlew :audit-service:quarkusDev > /tmp/audit-service.log 2>&1 &
AUDIT_SERVICE_PID=$!
echo "Audit-service PID: $AUDIT_SERVICE_PID"

# Wait for audit-service to start
sleep 8

# API Gateway
echo "Starting api-gateway on port 8080..."
./gradlew :api-gateway:quarkusDev > /tmp/api-gateway.log 2>&1 &
GATEWAY_PID=$!
echo "API-gateway PID: $GATEWAY_PID"

# ============================================================================
# Step 4: Health Checks
# ============================================================================
echo -e "\n${YELLOW}[4/4] Verifying services...${NC}"

sleep 5

# Check infrastructure
echo "Checking infrastructure..."
docker-compose ps 2>/dev/null | grep -q "Up" && echo -e "${GREEN}✓ Infrastructure containers running${NC}" || echo -e "${RED}✗ Infrastructure check failed${NC}"

# Check auth-service
if curl -s http://localhost:8081/q/health/ready > /dev/null 2>&1; then
    echo -e "${GREEN}✓ auth-service healthy (http://localhost:8081)${NC}"
else
    echo -e "${YELLOW}⏳ auth-service starting (http://localhost:8081)${NC}"
fi

# Check audit-service
if curl -s http://localhost:8088/q/health/ready > /dev/null 2>&1; then
    echo -e "${GREEN}✓ audit-service healthy (http://localhost:8088)${NC}"
else
    echo -e "${YELLOW}⏳ audit-service starting (http://localhost:8088)${NC}"
fi

# Check api-gateway
if curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ api-gateway healthy (http://localhost:8080)${NC}"
else
    echo -e "${YELLOW}⏳ api-gateway starting (http://localhost:8080)${NC}"
fi

# ============================================================================
# Summary
# ============================================================================
echo -e "\n${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    STARTUP COMPLETE                           ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"

cat << EOF

${GREEN}Service Status:${NC}

Infrastructure (Docker):
  • PostgreSQL:     localhost:5432
  • Redis:          localhost:6379
  • RabbitMQ:       localhost:5672 (management: localhost:15672)
  • Consul:         localhost:8500

Monitoring:
  • Prometheus:     http://localhost:9090
  • Grafana:        http://localhost:3000 (admin/admin)

Microservices:
  • Auth Service:   http://localhost:8081
  • Audit Service:  http://localhost:8088
  • API Gateway:    http://localhost:8080

${YELLOW}Logs Location:${NC}
  • Auth Service:   /tmp/auth-service.log
  • Audit Service:  /tmp/audit-service.log
  • API Gateway:    /tmp/api-gateway.log

${YELLOW}To view logs in real-time:${NC}
  tail -f /tmp/auth-service.log
  tail -f /tmp/audit-service.log
  tail -f /tmp/api-gateway.log

${YELLOW}To stop all services:${NC}
  ./stop-local.sh

EOF

# Save PIDs to file for stop script
cat > /tmp/hospital-services.pids << PID_EOF
AUTH_SERVICE_PID=$AUTH_SERVICE_PID
AUDIT_SERVICE_PID=$AUDIT_SERVICE_PID
GATEWAY_PID=$GATEWAY_PID
PID_EOF

echo -e "${GREEN}System startup complete!${NC}"
